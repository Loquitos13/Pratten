package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.Tenant;
import beringela.software.dto.TenantDtos.CreateTenantRequest;
import beringela.software.repository.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant create(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Slug already in use: " + request.slug());
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setVatNumber(request.vatNumber());
        tenant.setAddress(request.address());
        if (StringUtils.hasText(request.currency())) {
            tenant.setCurrency(request.currency());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant get(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Tenant", id));
    }
}
