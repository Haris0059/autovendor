<?php

/**
 * Plugin Name: OLX Integracija
 * Description: REST endpoint exposing WooCommerce products, categories, tags, attributes, and media for external sync.
 * Version: 1.0.3
 * Author: BitSync d.o.o.
 */

if (!defined('ABSPATH')) {
    exit;
}

final class OLX_Sync_Endpoint
{
    private $namespace = 'integracija/v1';
    private $route = '/catalog';
    private $option_key = 'integracija_api_key';

    // Cache/update constants
    private $version_transient_key = 'olx_integracija_version_info';
    private $version_check_event = 'olx_integracija_daily_version_check';
    private $heartbeat_event = 'olx_integracija_heartbeat';

    private $version_endpoint = 'https://dashboard.integracija.ba/api/plugin/versions';
    private $activation_endpoint = 'https://dashboard.integracija.ba/api/plugin/activation';
    private $heartbeat_endpoint = 'https://dashboard.integracija.ba/api/plugin/heartbeat';

    public function __construct()
    {
        // REST rute
        add_action('rest_api_init', [$this, 'register_routes']);

        // API key generisanje
        add_action('admin_init', [$this, 'maybe_generate_api_key']);

        // Cron intervals
        add_filter('cron_schedules', [$this, 'add_five_minute_cron_schedule']);

        // Aktivacija / deaktivacija plugina
        register_activation_hook(__FILE__, [$this, 'activate_plugin']);
        register_deactivation_hook(__FILE__, [$this, 'deactivate_plugin']);

        // Admin notice - sada SAMO čita cache, bez remote poziva
        add_action('admin_notices', [$this, 'render_admin_notice']);

        // Auto-update u Plugins listi - sada SAMO čita cache
        add_filter('site_transient_update_plugins', [$this, 'check_for_update']);

        // Info popup "View version X details"
        add_filter('plugins_api', [$this, 'plugins_api_info'], 10, 3);

        // Cron handlers
        add_action($this->heartbeat_event, [$this, 'send_heartbeat']);
        add_action($this->version_check_event, [$this, 'refresh_version_info_cache']);
    }

    /**
     * Cron interval od 5 minuta za heartbeat
     */
    public function add_five_minute_cron_schedule($schedules)
    {
        if (!isset($schedules['five_minutes'])) {
            $schedules['five_minutes'] = [
                'interval' => 5 * 60,
                'display'  => __('Every 5 Minutes', 'olx-integracija'),
            ];
        }

        return $schedules;
    }

    /**
     * Vrati trenutnu verziju plugina iz headera
     */
    private function get_current_plugin_version()
    {
        $plugin_data = get_file_data(__FILE__, ['Version' => 'Version']);
        return isset($plugin_data['Version']) && $plugin_data['Version'] !== ''
            ? $plugin_data['Version']
            : '1.0.0';
    }

    /**
     * Centralizovan GET request sa kratkim timeoutom
     */
    private function remote_get_json($url, $timeout = 3)
    {
        $response = wp_remote_get($url, [
            'timeout'     => $timeout,
            'redirection' => 2,
            'sslverify'   => true,
            'headers'     => [
                'Accept' => 'application/json',
            ],
        ]);

        if (is_wp_error($response)) {
            error_log('OLX Integracija remote GET error: ' . $response->get_error_message());
            return false;
        }

        $code = (int) wp_remote_retrieve_response_code($response);
        $body = wp_remote_retrieve_body($response);

        if ($code < 200 || $code >= 300) {
            error_log('OLX Integracija remote GET bad status [' . $code . ']: ' . $body);
            return false;
        }

        $data = json_decode($body, true);
        if (!is_array($data)) {
            error_log('OLX Integracija remote GET invalid JSON: ' . $body);
            return false;
        }

        return $data;
    }

    /**
     * Centralizovan POST request sa kratkim timeoutom
     */
    private function remote_post_form($url, array $body, $timeout = 5)
    {
        $response = wp_remote_post($url, [
            'timeout' => $timeout,
            'body'    => $body,
            'headers' => [
                'Content-Type' => 'application/x-www-form-urlencoded',
                'Accept'       => 'application/json',
            ],
        ]);

        if (is_wp_error($response)) {
            error_log('OLX Integracija remote POST error: ' . $response->get_error_message());
            return false;
        }

        return $response;
    }

    /**
     * Dohvati i cache-aj version info.
     * OVO JE JEDINO MJESTO gdje se radi live versions request.
     */
    public function refresh_version_info_cache()
    {
        $data = $this->remote_get_json($this->version_endpoint, 3);

        if (!$data || !isset($data['latest_version'])) {
            return false;
        }

        $payload = [
            'fetched_at' => time(),
            'data'       => $data,
        ];

        set_transient($this->version_transient_key, $payload, DAY_IN_SECONDS);

        return true;
    }

    /**
     * Vrati version info iz cache-a.
     * Po želji može uraditi tihi refresh samo ako cache ne postoji i nismo u rizičnom kontekstu.
     */
    private function get_cached_version_info($allow_soft_refresh = false)
    {
        $cached = get_transient($this->version_transient_key);

        if (is_array($cached) && isset($cached['data']) && is_array($cached['data'])) {
            return $cached['data'];
        }

        if ($allow_soft_refresh && !$this->is_restricted_runtime_context()) {
            $ok = $this->refresh_version_info_cache();
            if ($ok) {
                $cached = get_transient($this->version_transient_key);
                if (is_array($cached) && isset($cached['data']) && is_array($cached['data'])) {
                    return $cached['data'];
                }
            }
        }

        return null;
    }

    /**
     * Konteksti u kojima NE želimo fallback live update pozive
     */
    private function is_restricted_runtime_context()
    {
        if ((defined('WP_CLI') && WP_CLI) || (defined('DOING_CRON') && DOING_CRON) || wp_doing_ajax()) {
            return true;
        }

        return false;
    }

    /**
     * Admin notice poruka ako postoji nova verzija
     * Sada koristi isključivo cache.
     */
    public function render_admin_notice()
    {
        if (!is_admin()) {
            return;
        }

        $data = $this->get_cached_version_info(false);
        if (!$data || !isset($data['latest_version'], $data['update_message'])) {
            return;
        }

        $current_version = $this->get_current_plugin_version();

        if (version_compare($current_version, $data['latest_version'], '<')) {
            echo '<div class="notice notice-warning is-dismissible">';
            echo '<p><strong>' . esc_html($data['update_message']) . '</strong></p>';
            echo '</div>';
        }
    }

    /**
     * Hook za WordPress update sistem (Plugins list)
     * Sada koristi SAMO cache.
     */
    public function check_for_update($transient)
    {
        if (empty($transient->checked)) {
            return $transient;
        }

        $plugin_file = plugin_basename(__FILE__);
        $current_version = $this->get_current_plugin_version();

        $data = $this->get_cached_version_info(false);
        if (!$data || !isset($data['latest_version'], $data['download_url'])) {
            return $transient;
        }

        if (version_compare($current_version, $data['latest_version'], '<')) {
            $obj = new stdClass();
            $obj->slug        = 'olx-integracija';
            $obj->plugin      = $plugin_file;
            $obj->new_version = $data['latest_version'];
            $obj->url         = 'https://dashboard.integracija.ba';
            $obj->package     = $data['download_url'];

            $transient->response[$plugin_file] = $obj;
        }

        return $transient;
    }

    /**
     * Plugin info – "View version X details" popup
     * Ovdje je OK uraditi soft refresh jer korisnik eksplicitno otvara detalje,
     * ali i dalje izbjegavamo to u cron/WP-CLI kontekstima.
     */
    public function plugins_api_info($res, $action, $args)
    {
        if ($action !== 'plugin_information') {
            return $res;
        }

        if (!isset($args->slug) || $args->slug !== 'olx-integracija') {
            return $res;
        }

        $data = $this->get_cached_version_info(true);
        if (!$data || !isset($data['latest_version'])) {
            return $res;
        }

        $plugin_name = !empty($data['plugin_name']) ? $data['plugin_name'] : 'OLX Integracija';
        $description = !empty($data['description']) ? $data['description'] : 'REST endpoint za izvoz WooCommerce kataloga i integraciju sa dashboard.integracija.ba.';
        $changelog   = !empty($data['changelog']) ? $data['changelog'] : 'Najnovije izmjene vidi u dashboard.integracija.ba.';

        $info = new stdClass();
        $info->name     = $plugin_name;
        $info->slug     = 'olx-integracija';
        $info->version  = $data['latest_version'];
        $info->author   = '<a href="https://bitsync.ba">BitSync d.o.o.</a>';
        $info->homepage = 'https://dashboard.integracija.ba';

        $info->sections = [
            'description' => nl2br($description),
            'changelog'   => nl2br($changelog),
        ];

        $info->download_link = isset($data['download_url']) ? $data['download_url'] : '';

        return $info;
    }

    /**
     * Registracija REST ruta
     */
    public function register_routes()
    {
        register_rest_route(
            $this->namespace,
            $this->route,
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'handle_request'],
                'permission_callback' => [$this, 'check_permission'],
                'args' => [
                    'per_page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 50,
                    ],
                    'page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 1,
                    ],
                    'updated_after' => [
                        'type'              => 'string',
                        'sanitize_callback' => 'sanitize_text_field',
                        'description'       => 'ISO8601 datetime to filter products updated after the given timestamp',
                    ],
                ],
            ]
        );

        register_rest_route(
            $this->namespace,
            '/catalog-hashes',
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'get_catalog_hashes'],
                'permission_callback' => [$this, 'check_permission'],
                'args' => [
                    'per_page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 200,
                    ],
                    'page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 1,
                    ],
                    'updated_after' => [
                        'type'              => 'string',
                        'sanitize_callback' => 'sanitize_text_field',
                        'description'       => 'ISO8601 datetime to filter products updated after the given timestamp',
                    ],
                ],
            ]
        );

        register_rest_route(
            $this->namespace,
            '/catalog-stock',
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'get_catalog_stock'],
                'permission_callback' => [$this, 'check_permission'],
                'args' => [
                    'per_page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 200,
                    ],
                    'page' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'default'           => 1,
                    ],
                    'updated_after' => [
                        'type'              => 'string',
                        'sanitize_callback' => 'sanitize_text_field',
                        'description'       => 'ISO8601 datetime to filter products updated after the given timestamp',
                    ],
                ],
            ]
        );

        register_rest_route(
            $this->namespace,
            '/categories',
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'get_categories_hierarchy'],
                'permission_callback' => [$this, 'check_permission'],
            ]
        );

        register_rest_route(
            $this->namespace,
            '/attributes',
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'get_all_attributes'],
                'permission_callback' => [$this, 'check_permission'],
            ]
        );

        register_rest_route(
            $this->namespace,
            '/product/(?P<id>\d+)',
            [
                'methods'             => WP_REST_Server::READABLE,
                'callback'            => [$this, 'get_product_by_id'],
                'permission_callback' => [$this, 'check_permission'],
                'args' => [
                    'id' => [
                        'type'              => 'integer',
                        'sanitize_callback' => 'absint',
                        'required'          => true,
                    ],
                ],
            ]
        );
    }

    /**
     * Permission check
     */
    public function check_permission(WP_REST_Request $request)
    {
        $stored = (string) get_option($this->option_key);

        if ($stored === '') {
            return false;
        }

        $provided = (string) $request->get_header('x-integracija-api-key');

        if ($provided === '') {
            $provided = (string) $request->get_header('integracija-api-key');
        }

        if ($provided === '') {
            $provided = (string) $request->get_header('integracija_api_key');
        }

        if ($provided === '') {
            $provided = (string) $request->get_param('api_key');
        }

        return $provided !== '' && hash_equals($stored, $provided);
    }

    public function maybe_generate_api_key()
    {
        if (!get_option($this->option_key)) {
            $key = wp_generate_password(32, false);
            update_option($this->option_key, $key);
        }
    }

    /**
     * Aktivacija plugina
     */
    public function activate_plugin()
    {
        $api_key = get_option($this->option_key);

        if (!$api_key) {
            $api_key = wp_generate_password(32, false);
            update_option($this->option_key, $api_key);
        }

        $site_url = get_site_url();
        $plugin_version = $this->get_current_plugin_version();

        $data = [
            'site_url'       => $site_url,
            'plugin_version' => $plugin_version,
            'api_key'        => $api_key,
        ];

        $response = $this->remote_post_form($this->activation_endpoint, $data, 5);

        if ($response === false) {
            error_log('Plugin activation error.');
        } else {
            error_log('Plugin activation response: ' . wp_remote_retrieve_body($response));
        }

        if (!wp_next_scheduled($this->heartbeat_event)) {
            wp_schedule_event(time() + 300, 'five_minutes', $this->heartbeat_event);
        }

        if (!wp_next_scheduled($this->version_check_event)) {
            wp_schedule_event(time() + 600, 'daily', $this->version_check_event);
        }

        $this->refresh_version_info_cache();
        $this->send_heartbeat();
    }

    /**
     * Deaktivacija plugina
     */
    public function deactivate_plugin()
    {
        $this->send_heartbeat('inactive');

        while ($timestamp = wp_next_scheduled($this->heartbeat_event)) {
            wp_unschedule_event($timestamp, $this->heartbeat_event);
        }

        while ($timestamp = wp_next_scheduled($this->version_check_event)) {
            wp_unschedule_event($timestamp, $this->version_check_event);
        }

        delete_transient($this->version_transient_key);
    }

    /**
     * Slanje heartbeat zahtjeva
     */
    public function send_heartbeat($status = 'active')
    {
        $api_key = get_option($this->option_key);
        $site_url = get_site_url();
        $plugin_version = $this->get_current_plugin_version();

        $body = [
            'site_url'       => $site_url,
            'plugin_version' => $plugin_version,
            'api_key'        => $api_key,
            'status'         => $status,
        ];

        $response = $this->remote_post_form($this->heartbeat_endpoint, $body, 5);

        if ($response === false) {
            error_log('Plugin heartbeat error.');
        } else {
            error_log('Plugin heartbeat response: ' . wp_remote_retrieve_body($response));
        }
    }

    public function handle_request(WP_REST_Request $request)
    {
        $per_page      = $request->get_param('per_page');
        $page          = $request->get_param('page');
        $updated_after = $request->get_param('updated_after');

        $args = [
            'status'  => ['publish', 'draft', 'pending'],
            'limit'   => $per_page,
            'page'    => $page,
            'orderby' => 'ID',
            'order'   => 'ASC',
            'return'  => 'objects',
        ];

        if ($updated_after) {
            $args['date_modified'] = $updated_after;
        }

        $products = wc_get_products($args);

        $data = [
            'products'   => array_map([$this, 'map_product'], $products),
            'categories' => $this->get_categories(),
            'tags'       => $this->get_tags(),
            'attributes' => $this->get_attributes(),
            'api_key'    => 'stored_in_db',
        ];

        return rest_ensure_response($data);
    }

    public function get_catalog_stock(WP_REST_Request $request)
    {
        $per_page      = $request->get_param('per_page');
        $page          = $request->get_param('page');
        $updated_after = $request->get_param('updated_after');

        $args = [
            'status'  => ['publish', 'draft', 'pending'],
            'limit'   => $per_page,
            'page'    => $page,
            'orderby' => 'ID',
            'order'   => 'ASC',
            'return'  => 'objects',
        ];

        if ($updated_after) {
            $args['date_modified'] = $updated_after;
        }

        $products = wc_get_products($args);

        $items = array_map(function ($product) {
            return [
                'id'        => (int) $product->get_id(),
                'stock'     => (string) $product->get_stock_status(),
                'stock_qty' => $product->get_stock_quantity(),
            ];
        }, $products);

        return rest_ensure_response([
            'products' => $items,
            'page'     => (int) $page,
            'per_page' => (int) $per_page,
            'count'    => count($items),
        ]);
    }

    private function hash_norm_text($v): string
    {
        $s = (string) $v;
        $s = wp_strip_all_tags($s, true);
        $s = html_entity_decode($s, ENT_QUOTES | ENT_HTML5, 'UTF-8');
        $s = preg_replace('/\s+/u', ' ', $s);
        return trim($s);
    }

    private function hash_norm_num($v): string
    {
        if ($v === null || $v === '') {
            return '';
        }

        return rtrim(rtrim(number_format((float) $v, 4, '.', ''), '0'), '.');
    }

    private function compute_product_hash(WC_Product $product, array $images): string
    {
        $imgUrls = array_values(array_filter(array_map(function ($img) {
            return isset($img['src']) ? (string) $img['src'] : '';
        }, $images)));

        sort($imgUrls, SORT_STRING);

        $payload = [
            'id'                => (int) $product->get_id(),
            'type'              => (string) $product->get_type(),
            'status'            => (string) $product->get_status(),
            'sku'               => (string) $product->get_sku(),
            'name'              => $this->hash_norm_text($product->get_name()),
            'price'             => $this->hash_norm_num($product->get_price()),
            'regular_price'     => $this->hash_norm_num($product->get_regular_price()),
            'sale_price'        => $this->hash_norm_num($product->get_sale_price()),
            'description'       => $this->hash_norm_text($product->get_description()),
            'short_description' => $this->hash_norm_text($product->get_short_description()),
            'stock'             => (string) $product->get_stock_status(),
            'stock_qty'         => $this->hash_norm_num($product->get_stock_quantity()),
            'images'            => $imgUrls,
        ];

        $json = wp_json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return hash('sha256', $json ?: '');
    }

    public function get_catalog_hashes(WP_REST_Request $request)
    {
        $per_page      = $request->get_param('per_page');
        $page          = $request->get_param('page');
        $updated_after = $request->get_param('updated_after');

        $args = [
            'status'  => ['publish', 'draft', 'pending'],
            'limit'   => $per_page,
            'page'    => $page,
            'orderby' => 'ID',
            'order'   => 'ASC',
            'return'  => 'objects',
        ];

        if ($updated_after) {
            $args['date_modified'] = $updated_after;
        }

        $products = wc_get_products($args);

        $items = array_map(function ($product) {
            $images = array_map(function ($img) {
                return [
                    'id'   => (int) $img['id'],
                    'src'  => esc_url_raw($img['src']),
                    'name' => $img['name'],
                    'alt'  => get_post_meta($img['id'], '_wp_attachment_image_alt', true),
                ];
            }, array_values($product->get_gallery_image_ids() ? array_map('wc_get_product_attachment_props', $product->get_gallery_image_ids()) : []));

            $featured_id = $product->get_image_id();
            if ($featured_id) {
                $featured_props = wc_get_product_attachment_props($featured_id);
                array_unshift($images, [
                    'id'   => $featured_id,
                    'src'  => esc_url_raw($featured_props['url']),
                    'name' => $featured_props['title'],
                    'alt'  => get_post_meta($featured_id, '_wp_attachment_image_alt', true),
                ]);
            }

            $hash = $this->compute_product_hash($product, $images);

            return [
                'id'   => (int) $product->get_id(),
                'hash' => $hash,
            ];
        }, $products);

        return rest_ensure_response([
            'products' => $items,
            'page'     => (int) $page,
            'per_page' => (int) $per_page,
            'count'    => count($items),
        ]);
    }

    private function map_product(WC_Product $product)
    {
        $images = array_map(function ($img) {
            return [
                'id'   => (int) $img['id'],
                'src'  => esc_url_raw($img['src']),
                'name' => $img['name'],
                'alt'  => get_post_meta($img['id'], '_wp_attachment_image_alt', true),
            ];
        }, array_values($product->get_gallery_image_ids() ? array_map('wc_get_product_attachment_props', $product->get_gallery_image_ids()) : []));

        $featured_id = $product->get_image_id();
        if ($featured_id) {
            $featured_props = wc_get_product_attachment_props($featured_id);
            array_unshift($images, [
                'id'   => $featured_id,
                'src'  => esc_url_raw($featured_props['url']),
                'name' => $featured_props['title'],
                'alt'  => get_post_meta($featured_id, '_wp_attachment_image_alt', true),
            ]);
        }

        $hash = $this->compute_product_hash($product, $images);

        $attrs = [];
        foreach ($product->get_attributes() as $key => $attr) {
            $taxName = method_exists($attr, 'get_name') ? $attr->get_name() : $key;
            $optionsOut = [];

            if (method_exists($attr, 'is_taxonomy') && $attr->is_taxonomy()) {
                $names = wc_get_product_terms($product->get_id(), $taxName, ['fields' => 'names']);
                if (is_array($names)) {
                    $optionsOut = array_values(array_filter(array_map('strval', $names), 'strlen'));
                }
            } else {
                $raw = $attr->get_options();
                if (is_array($raw)) {
                    $optionsOut = array_values(array_filter(array_map('strval', $raw), 'strlen'));
                } elseif (is_string($raw) && trim($raw) !== '') {
                    $optionsOut = [trim($raw)];
                }
            }

            $attrs[] = [
                'name'      => $taxName,
                'label'     => wc_attribute_label($taxName),
                'options'   => $optionsOut,
                'visible'   => (bool) $attr->get_visible(),
                'variation' => (bool) $attr->get_variation(),
            ];
        }

        $variations = [];
        if ($product->is_type('variable')) {
            foreach ($product->get_children() as $child_id) {
                $variation = wc_get_product($child_id);
                if (!$variation) {
                    continue;
                }

                $variations[] = [
                    'id'            => $variation->get_id(),
                    'sku'           => $variation->get_sku(),
                    'price'         => $variation->get_price(),
                    'regular_price' => $variation->get_regular_price(),
                    'sale_price'    => $variation->get_sale_price(),
                    'stock_status'  => $variation->get_stock_status(),
                    'stock_qty'     => $variation->get_stock_quantity(),
                    'attributes'    => $variation->get_attributes(),
                    'weight'        => $variation->get_weight(),
                    'dimensions'    => [
                        'length' => $variation->get_length(),
                        'width'  => $variation->get_width(),
                        'height' => $variation->get_height(),
                    ],
                ];
            }
        }

        return [
            'id'                => $product->get_id(),
            'hash'              => $hash,
            'type'              => $product->get_type(),
            'name'              => $product->get_name(),
            'slug'              => $product->get_slug(),
            'sku'               => $product->get_sku(),
            'status'            => $product->get_status(),
            'price'             => $product->get_price(),
            'regular_price'     => $product->get_regular_price(),
            'sale_price'        => $product->get_sale_price(),
            'stock_status'      => $product->get_stock_status(),
            'stock_qty'         => $product->get_stock_quantity(),
            'categories'        => array_map(function ($term) {
                return ['id' => $term['id'], 'name' => $term['name'], 'slug' => $term['slug']];
            }, $product->get_category_ids() ? array_map(function ($id) {
                $term = get_term($id, 'product_cat');
                return ['id' => $id, 'name' => $term ? $term->name : '', 'slug' => $term ? $term->slug : ''];
            }, $product->get_category_ids()) : []),
            'tags'              => array_map(function ($id) {
                $term = get_term($id, 'product_tag');
                return ['id' => $id, 'name' => $term ? $term->name : '', 'slug' => $term ? $term->slug : ''];
            }, $product->get_tag_ids()),
            'attributes'        => $attrs,
            'variations'        => $variations,
            'images'            => $images,
            'description'       => $product->get_description(),
            'short_description' => $product->get_short_description(),
            'weight'            => $product->get_weight(),
            'dimensions'        => [
                'length' => $product->get_length(),
                'width'  => $product->get_width(),
                'height' => $product->get_height(),
            ],
            'shipping_class'    => $product->get_shipping_class(),
            'date_created'      => $product->get_date_created() ? $product->get_date_created()->date(DATE_ATOM) : null,
            'date_modified'     => $product->get_date_modified() ? $product->get_date_modified()->date(DATE_ATOM) : null,
            'permalink'         => $product->get_permalink(),
        ];
    }

    private function get_categories()
    {
        $terms = get_terms(['taxonomy' => 'product_cat', 'hide_empty' => false]);

        return array_map(function ($t) {
            return [
                'id'          => $t->term_id,
                'name'        => $t->name,
                'slug'        => $t->slug,
                'parent'      => $t->parent,
                'description' => $t->description,
            ];
        }, is_array($terms) ? $terms : []);
    }

    public function get_categories_hierarchy()
    {
        $categories = get_terms([
            'taxonomy'   => 'product_cat',
            'hide_empty' => false,
        ]);

        $hierarchy = [];
        foreach ($categories as $category) {
            if ($category->parent === 0) {
                $hierarchy[] = $this->build_category_hierarchy($category, $categories);
            }
        }

        return rest_ensure_response($hierarchy);
    }

    private function build_category_hierarchy($category, $all_categories)
    {
        $children = array_filter($all_categories, function ($cat) use ($category) {
            return $cat->parent === $category->term_id;
        });

        $child_hierarchy = [];
        foreach ($children as $child) {
            $child_hierarchy[] = $this->build_category_hierarchy($child, $all_categories);
        }

        return [
            'id'       => $category->term_id,
            'name'     => $category->name,
            'slug'     => $category->slug,
            'children' => $child_hierarchy,
        ];
    }

    private function get_tags()
    {
        $terms = get_terms(['taxonomy' => 'product_tag', 'hide_empty' => false]);

        return array_map(function ($t) {
            return [
                'id'   => $t->term_id,
                'name' => $t->name,
                'slug' => $t->slug,
            ];
        }, is_array($terms) ? $terms : []);
    }

    private function get_attributes()
    {
        $taxes = wc_get_attribute_taxonomies();
        $attributes = [];

        foreach ($taxes as $tax) {
            $taxonomy = wc_attribute_taxonomy_name($tax->attribute_name);
            $terms = get_terms(['taxonomy' => $taxonomy, 'hide_empty' => false]);

            $attributes[] = [
                'id'       => (int) $tax->attribute_id,
                'name'     => $tax->attribute_label,
                'slug'     => $tax->attribute_name,
                'type'     => $tax->attribute_type,
                'order_by' => $tax->attribute_orderby,
                'terms'    => array_map(function ($t) {
                    return [
                        'id'   => $t->term_id,
                        'name' => $t->name,
                        'slug' => $t->slug,
                    ];
                }, is_array($terms) ? $terms : []),
            ];
        }

        return $attributes;
    }

    public function get_all_attributes()
    {
        return rest_ensure_response($this->get_attributes());
    }

    public function get_product_by_id(WP_REST_Request $request)
    {
        $id = $request->get_param('id');
        $product = wc_get_product($id);

        if (!$product) {
            return new WP_Error('product_not_found', 'Product not found', ['status' => 404]);
        }

        return rest_ensure_response($this->map_product($product));
    }
}

new OLX_Sync_Endpoint();
