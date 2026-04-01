<?php

/**
 * Plugin Name: AutoVendor
 * Description: REST endpoint exposing WooCommerce products, categories, tags, attributes, and media for AutoVendor dashboard sync.
 * Version: 1.0.0
 * Author: AutoVendor
 * Requires Plugins: woocommerce
 */

if (!defined('ABSPATH')) {
    exit;
}

final class AutoVendor_Endpoint
{
    private $namespace = 'autovendor/v1';
    private $option_key = 'autovendor_api_key';
    private $heartbeat_event = 'autovendor_heartbeat';
    private $heartbeat_endpoint = '';

    public function __construct()
    {
        add_action('rest_api_init', [$this, 'register_routes']);
        add_action('admin_init', [$this, 'maybe_generate_api_key']);
        add_action('admin_menu', [$this, 'add_settings_page']);
        add_filter('cron_schedules', [$this, 'add_five_minute_schedule']);

        register_activation_hook(__FILE__, [$this, 'activate']);
        register_deactivation_hook(__FILE__, [$this, 'deactivate']);

        // Heartbeat cron handler
        add_action($this->heartbeat_event, [$this, 'send_heartbeat']);

        // Webhooks — notify AutoVendor on product changes
        add_action('woocommerce_new_product', [$this, 'on_product_created'], 10, 2);
        add_action('woocommerce_update_product', [$this, 'on_product_updated'], 10, 2);
        add_action('woocommerce_before_delete_product', [$this, 'on_product_deleted'], 10, 1);
        add_action('woocommerce_trash_product', [$this, 'on_product_deleted'], 10, 1);
    }

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    public function activate()
    {
        $this->maybe_generate_api_key();

        if (!wp_next_scheduled($this->heartbeat_event)) {
            wp_schedule_event(time() + 300, 'five_minutes', $this->heartbeat_event);
        }

        $this->send_heartbeat('active');
    }

    public function deactivate()
    {
        $this->send_heartbeat('inactive');

        while ($timestamp = wp_next_scheduled($this->heartbeat_event)) {
            wp_unschedule_event($timestamp, $this->heartbeat_event);
        }
    }

    public function add_five_minute_schedule($schedules)
    {
        if (!isset($schedules['five_minutes'])) {
            $schedules['five_minutes'] = [
                'interval' => 5 * 60,
                'display'  => __('Every 5 Minutes', 'autovendor'),
            ];
        }
        return $schedules;
    }

    // ──────────────────────────────────────────────
    // Auth
    // ──────────────────────────────────────────────

    public function maybe_generate_api_key()
    {
        if (!get_option($this->option_key)) {
            update_option($this->option_key, wp_generate_password(32, false));
        }
    }

    public function check_permission(WP_REST_Request $request)
    {
        $stored = (string) get_option($this->option_key);
        if ($stored === '') {
            return false;
        }

        $provided = (string) $request->get_header('x-autovendor-api-key');
        if ($provided === '') {
            $provided = (string) $request->get_param('api_key');
        }

        return $provided !== '' && hash_equals($stored, $provided);
    }

    // ──────────────────────────────────────────────
    // REST Routes
    // ──────────────────────────────────────────────

    public function register_routes()
    {
        $pagination_args = [
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
                'description'       => 'ISO8601 datetime — only return products modified after this timestamp',
            ],
        ];

        $hash_pagination_args = $pagination_args;
        $hash_pagination_args['per_page']['default'] = 200;

        // Full catalog (products + categories + tags + attributes)
        register_rest_route($this->namespace, '/catalog', [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => [$this, 'get_catalog'],
            'permission_callback' => [$this, 'check_permission'],
            'args'                => array_merge($pagination_args, [
                'ids' => [
                    'type'              => 'string',
                    'sanitize_callback' => 'sanitize_text_field',
                    'description'       => 'Comma-separated product IDs to fetch',
                ],
            ]),
        ]);

        // Hashes only (for change detection)
        register_rest_route($this->namespace, '/catalog-hashes', [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => [$this, 'get_catalog_hashes'],
            'permission_callback' => [$this, 'check_permission'],
            'args'                => $hash_pagination_args,
        ]);

        // Stock only
        register_rest_route($this->namespace, '/catalog-stock', [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => [$this, 'get_catalog_stock'],
            'permission_callback' => [$this, 'check_permission'],
            'args'                => $hash_pagination_args,
        ]);

        // Single product
        register_rest_route($this->namespace, '/product/(?P<id>\d+)', [
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
        ]);

        // Category hierarchy
        register_rest_route($this->namespace, '/categories', [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => [$this, 'get_categories_hierarchy'],
            'permission_callback' => [$this, 'check_permission'],
        ]);

        // Attributes + terms
        register_rest_route($this->namespace, '/attributes', [
            'methods'             => WP_REST_Server::READABLE,
            'callback'            => [$this, 'get_all_attributes'],
            'permission_callback' => [$this, 'check_permission'],
        ]);
    }

    // ──────────────────────────────────────────────
    // Product queries
    // ──────────────────────────────────────────────

    private function query_products(WP_REST_Request $request, $default_per_page = null)
    {
        $per_page = $default_per_page ?? $request->get_param('per_page');
        $page = $request->get_param('page');
        $updated_after = $request->get_param('updated_after');
        $ids = $request->get_param('ids');

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

        if ($ids) {
            $id_list = array_filter(array_map('absint', explode(',', $ids)));
            if (!empty($id_list)) {
                $args['include'] = $id_list;
                $args['limit'] = count($id_list);
            }
        }

        return wc_get_products($args);
    }

    // ──────────────────────────────────────────────
    // Endpoint handlers
    // ──────────────────────────────────────────────

    public function get_catalog(WP_REST_Request $request)
    {
        $products = $this->query_products($request);

        return rest_ensure_response([
            'products'   => array_map([$this, 'map_product'], $products),
            'categories' => $this->get_categories(),
            'tags'       => $this->get_tags(),
            'attributes' => $this->get_attributes(),
            'page'       => (int) $request->get_param('page'),
            'per_page'   => (int) $request->get_param('per_page'),
            'count'      => count($products),
        ]);
    }

    public function get_catalog_hashes(WP_REST_Request $request)
    {
        $products = $this->query_products($request);

        $items = array_map(function ($product) {
            $images = $this->get_product_images($product);
            return [
                'id'   => (int) $product->get_id(),
                'hash' => $this->compute_product_hash($product, $images),
            ];
        }, $products);

        return rest_ensure_response([
            'products' => $items,
            'page'     => (int) $request->get_param('page'),
            'per_page' => (int) $request->get_param('per_page'),
            'count'    => count($items),
        ]);
    }

    public function get_catalog_stock(WP_REST_Request $request)
    {
        $products = $this->query_products($request);

        $items = array_map(function ($product) {
            return [
                'id'        => (int) $product->get_id(),
                'stock'     => (string) $product->get_stock_status(),
                'stock_qty' => $product->get_stock_quantity(),
            ];
        }, $products);

        return rest_ensure_response([
            'products' => $items,
            'page'     => (int) $request->get_param('page'),
            'per_page' => (int) $request->get_param('per_page'),
            'count'    => count($items),
        ]);
    }

    public function get_product_by_id(WP_REST_Request $request)
    {
        $product = wc_get_product($request->get_param('id'));

        if (!$product) {
            return new WP_Error('product_not_found', 'Product not found', ['status' => 404]);
        }

        return rest_ensure_response($this->map_product($product));
    }

    public function get_categories_hierarchy()
    {
        $categories = get_terms([
            'taxonomy'   => 'product_cat',
            'hide_empty' => false,
        ]);

        if (!is_array($categories)) {
            return rest_ensure_response([]);
        }

        $hierarchy = [];
        foreach ($categories as $cat) {
            if ($cat->parent === 0) {
                $hierarchy[] = $this->build_category_tree($cat, $categories);
            }
        }

        return rest_ensure_response($hierarchy);
    }

    public function get_all_attributes()
    {
        return rest_ensure_response($this->get_attributes());
    }

    // ──────────────────────────────────────────────
    // Product mapping
    // ──────────────────────────────────────────────

    private function map_product(WC_Product $product)
    {
        $images = $this->get_product_images($product);
        $hash = $this->compute_product_hash($product, $images);

        $attrs = [];
        foreach ($product->get_attributes() as $key => $attr) {
            $tax_name = method_exists($attr, 'get_name') ? $attr->get_name() : $key;

            if (method_exists($attr, 'is_taxonomy') && $attr->is_taxonomy()) {
                $names = wc_get_product_terms($product->get_id(), $tax_name, ['fields' => 'names']);
                $options = is_array($names) ? array_values(array_filter(array_map('strval', $names), 'strlen')) : [];
            } else {
                $raw = $attr->get_options();
                if (is_array($raw)) {
                    $options = array_values(array_filter(array_map('strval', $raw), 'strlen'));
                } elseif (is_string($raw) && trim($raw) !== '') {
                    $options = [trim($raw)];
                } else {
                    $options = [];
                }
            }

            $attrs[] = [
                'name'      => $tax_name,
                'label'     => wc_attribute_label($tax_name),
                'options'   => $options,
                'visible'   => (bool) $attr->get_visible(),
                'variation' => (bool) $attr->get_variation(),
            ];
        }

        $variations = [];
        if ($product->is_type('variable')) {
            foreach ($product->get_children() as $child_id) {
                $variation = wc_get_product($child_id);
                if (!$variation) continue;

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

        $category_ids = $product->get_category_ids();
        $categories = array_map(function ($id) {
            $term = get_term($id, 'product_cat');
            return [
                'id'   => $id,
                'name' => $term ? $term->name : '',
                'slug' => $term ? $term->slug : '',
            ];
        }, is_array($category_ids) ? $category_ids : []);

        $tag_ids = $product->get_tag_ids();
        $tags = array_map(function ($id) {
            $term = get_term($id, 'product_tag');
            return [
                'id'   => $id,
                'name' => $term ? $term->name : '',
                'slug' => $term ? $term->slug : '',
            ];
        }, is_array($tag_ids) ? $tag_ids : []);

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
            'categories'        => $categories,
            'tags'              => $tags,
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

    // ──────────────────────────────────────────────
    // Images
    // ──────────────────────────────────────────────

    private function get_product_images(WC_Product $product)
    {
        $images = [];

        $featured_id = $product->get_image_id();
        if ($featured_id) {
            $props = wc_get_product_attachment_props($featured_id);
            $images[] = [
                'id'   => (int) $featured_id,
                'src'  => esc_url_raw($props['url']),
                'name' => $props['title'],
                'alt'  => get_post_meta($featured_id, '_wp_attachment_image_alt', true),
            ];
        }

        $gallery_ids = $product->get_gallery_image_ids();
        if (!empty($gallery_ids)) {
            foreach ($gallery_ids as $img_id) {
                $props = wc_get_product_attachment_props($img_id);
                $images[] = [
                    'id'   => (int) $img_id,
                    'src'  => esc_url_raw($props['url']),
                    'name' => $props['title'],
                    'alt'  => get_post_meta($img_id, '_wp_attachment_image_alt', true),
                ];
            }
        }

        return $images;
    }

    // ──────────────────────────────────────────────
    // Content hashing
    // ──────────────────────────────────────────────

    private function normalize_text($value)
    {
        $s = (string) $value;
        $s = wp_strip_all_tags($s, true);
        $s = html_entity_decode($s, ENT_QUOTES | ENT_HTML5, 'UTF-8');
        $s = preg_replace('/\s+/u', ' ', $s);
        return trim($s);
    }

    private function normalize_number($value)
    {
        if ($value === null || $value === '') return '';
        return rtrim(rtrim(number_format((float) $value, 4, '.', ''), '0'), '.');
    }

    private function compute_product_hash(WC_Product $product, array $images)
    {
        $img_urls = array_values(array_filter(array_map(function ($img) {
            return isset($img['src']) ? (string) $img['src'] : '';
        }, $images)));
        sort($img_urls, SORT_STRING);

        $payload = [
            'id'                => (int) $product->get_id(),
            'type'              => (string) $product->get_type(),
            'status'            => (string) $product->get_status(),
            'sku'               => (string) $product->get_sku(),
            'name'              => $this->normalize_text($product->get_name()),
            'price'             => $this->normalize_number($product->get_price()),
            'regular_price'     => $this->normalize_number($product->get_regular_price()),
            'sale_price'        => $this->normalize_number($product->get_sale_price()),
            'description'       => $this->normalize_text($product->get_description()),
            'short_description' => $this->normalize_text($product->get_short_description()),
            'stock'             => (string) $product->get_stock_status(),
            'stock_qty'         => $this->normalize_number($product->get_stock_quantity()),
            'images'            => $img_urls,
        ];

        $json = wp_json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return hash('sha256', $json ?: '');
    }

    // ──────────────────────────────────────────────
    // Taxonomy helpers
    // ──────────────────────────────────────────────

    private function get_categories()
    {
        $terms = get_terms(['taxonomy' => 'product_cat', 'hide_empty' => false]);
        if (!is_array($terms)) return [];

        return array_map(function ($t) {
            return [
                'id'          => $t->term_id,
                'name'        => $t->name,
                'slug'        => $t->slug,
                'parent'      => $t->parent,
                'description' => $t->description,
            ];
        }, $terms);
    }

    private function build_category_tree($category, $all)
    {
        $children = [];
        foreach ($all as $cat) {
            if ($cat->parent === $category->term_id) {
                $children[] = $this->build_category_tree($cat, $all);
            }
        }

        return [
            'id'       => $category->term_id,
            'name'     => $category->name,
            'slug'     => $category->slug,
            'children' => $children,
        ];
    }

    private function get_tags()
    {
        $terms = get_terms(['taxonomy' => 'product_tag', 'hide_empty' => false]);
        if (!is_array($terms)) return [];

        return array_map(function ($t) {
            return [
                'id'   => $t->term_id,
                'name' => $t->name,
                'slug' => $t->slug,
            ];
        }, $terms);
    }

    private function get_attributes()
    {
        $taxonomies = wc_get_attribute_taxonomies();
        $attributes = [];

        foreach ($taxonomies as $tax) {
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

    // ──────────────────────────────────────────────
    // Webhooks — notify AutoVendor on product changes
    // ──────────────────────────────────────────────

    public function on_product_created($product_id, $product)
    {
        $this->send_webhook('product.created', $product_id);
    }

    public function on_product_updated($product_id, $product)
    {
        $this->send_webhook('product.updated', $product_id);
    }

    public function on_product_deleted($product_id)
    {
        $this->send_webhook('product.deleted', $product_id);
    }

    private function send_webhook($event, $product_id)
    {
        $webhook_url = get_option('autovendor_webhook_url');
        if (empty($webhook_url)) return;

        $api_key = (string) get_option($this->option_key);

        wp_remote_post($webhook_url, [
            'timeout'   => 5,
            'blocking'  => false,
            'headers'   => [
                'Content-Type'        => 'application/json',
                'X-AutoVendor-Event'  => $event,
                'X-AutoVendor-Key'    => $api_key,
            ],
            'body' => wp_json_encode([
                'event'      => $event,
                'product_id' => (int) $product_id,
                'site_url'   => get_site_url(),
                'timestamp'  => gmdate('c'),
            ]),
        ]);
    }

    // ──────────────────────────────────────────────
    // Heartbeat
    // ──────────────────────────────────────────────

    public function send_heartbeat($status = 'active')
    {
        if (empty($this->heartbeat_endpoint)) return;

        $api_key = (string) get_option($this->option_key);

        wp_remote_post($this->heartbeat_endpoint, [
            'timeout' => 5,
            'body'    => [
                'site_url'       => get_site_url(),
                'plugin_version' => $this->get_plugin_version(),
                'api_key'        => $api_key,
                'status'         => $status,
            ],
        ]);
    }

    private function get_plugin_version()
    {
        $data = get_file_data(__FILE__, ['Version' => 'Version']);
        return !empty($data['Version']) ? $data['Version'] : '1.0.0';
    }

    // ──────────────────────────────────────────────
    // Admin settings page
    // ──────────────────────────────────────────────

    public function add_settings_page()
    {
        add_submenu_page(
            'woocommerce',
            'AutoVendor',
            'AutoVendor',
            'manage_woocommerce',
            'autovendor-settings',
            [$this, 'render_settings_page']
        );
    }

    public function render_settings_page()
    {
        if (!current_user_can('manage_woocommerce')) {
            wp_die('Unauthorized');
        }

        // Handle webhook URL save
        if (isset($_POST['autovendor_save_settings']) && check_admin_referer('autovendor_settings')) {
            $webhook_url = esc_url_raw(wp_unslash($_POST['autovendor_webhook_url'] ?? ''));
            update_option('autovendor_webhook_url', $webhook_url);
            echo '<div class="notice notice-success"><p>Settings saved.</p></div>';
        }

        // Handle API key regeneration
        if (isset($_POST['autovendor_regenerate_key']) && check_admin_referer('autovendor_settings')) {
            update_option($this->option_key, wp_generate_password(32, false));
            echo '<div class="notice notice-success"><p>API key regenerated.</p></div>';
        }

        $api_key = get_option($this->option_key);
        $webhook_url = get_option('autovendor_webhook_url', '');
        $site_url = get_site_url();
        $base_endpoint = rest_url($this->namespace);

        ?>
        <div class="wrap">
            <h1>AutoVendor Settings</h1>

            <div class="card" style="max-width: 700px; margin-top: 20px; padding: 20px;">
                <h2>Connection Info</h2>
                <table class="form-table">
                    <tr>
                        <th>Site URL</th>
                        <td><code><?php echo esc_html($site_url); ?></code></td>
                    </tr>
                    <tr>
                        <th>API Base</th>
                        <td><code><?php echo esc_html($base_endpoint); ?></code></td>
                    </tr>
                    <tr>
                        <th>API Key</th>
                        <td>
                            <code id="av-api-key"><?php echo esc_html($api_key); ?></code>
                            <button type="button" class="button button-small" onclick="navigator.clipboard.writeText(document.getElementById('av-api-key').textContent)">Copy</button>
                        </td>
                    </tr>
                    <tr>
                        <th>Plugin Version</th>
                        <td><?php echo esc_html($this->get_plugin_version()); ?></td>
                    </tr>
                </table>
            </div>

            <form method="post" style="max-width: 700px; margin-top: 20px;">
                <?php wp_nonce_field('autovendor_settings'); ?>

                <div class="card" style="padding: 20px;">
                    <h2>Webhook</h2>
                    <p>AutoVendor dashboard URL to notify on product changes. Leave empty to disable.</p>
                    <table class="form-table">
                        <tr>
                            <th><label for="autovendor_webhook_url">Webhook URL</label></th>
                            <td>
                                <input type="url" id="autovendor_webhook_url" name="autovendor_webhook_url"
                                       value="<?php echo esc_attr($webhook_url); ?>" class="regular-text"
                                       placeholder="https://your-autovendor-instance.com/api/webhook">
                            </td>
                        </tr>
                    </table>
                    <p>
                        <button type="submit" name="autovendor_save_settings" class="button button-primary">Save Settings</button>
                    </p>
                </div>
            </form>

            <form method="post" style="max-width: 700px; margin-top: 20px;">
                <?php wp_nonce_field('autovendor_settings'); ?>
                <div class="card" style="padding: 20px;">
                    <h2>Regenerate API Key</h2>
                    <p>This will invalidate the current key. You'll need to update it in your AutoVendor dashboard.</p>
                    <button type="submit" name="autovendor_regenerate_key" class="button"
                            onclick="return confirm('Regenerate API key? The current key will stop working.')">
                        Regenerate Key
                    </button>
                </div>
            </form>
        </div>
        <?php
    }
}

new AutoVendor_Endpoint();
