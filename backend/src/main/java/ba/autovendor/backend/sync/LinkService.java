package ba.autovendor.backend.sync;

import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.sync.dto.CreateProductLinkRequest;
import ba.autovendor.backend.sync.dto.ProductLinkResponse;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LinkService {

    private final ProductLinkRepository linkRepository;
    private final OlxAccountRepository accountRepository;
    private final WooStoreRepository storeRepository;

    public LinkService(ProductLinkRepository linkRepository,
                       OlxAccountRepository accountRepository,
                       WooStoreRepository storeRepository) {
        this.linkRepository = linkRepository;
        this.accountRepository = accountRepository;
        this.storeRepository = storeRepository;
    }

    public List<ProductLinkResponse> list(User user) {
        return linkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SyncMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductLinkResponse create(User user, CreateProductLinkRequest request) {
        accountRepository.findByIdAndUserId(request.olxAccountId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
        storeRepository.findByIdAndUserId(request.wooStoreId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        if (linkRepository.existsByWooStoreIdAndWooProductId(request.wooStoreId(), request.wooProductId())) {
            throw new IllegalArgumentException("Product already linked");
        }

        ProductLink link = new ProductLink(user.getId(), request.olxAccountId(), request.wooStoreId(),
                request.olxListingId(), request.wooProductId(), request.syncDirection());
        return SyncMapper.toResponse(linkRepository.save(link));
    }

    @Transactional
    public void delete(User user, Long id) {
        ProductLink link = linkRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Product link not found"));
        linkRepository.delete(link);
    }
}
