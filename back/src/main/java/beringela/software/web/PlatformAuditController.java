package beringela.software.web;

import beringela.software.dto.PlatformDtos.PlatformAuditEntry;
import beringela.software.service.PlatformService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/audit")
public class PlatformAuditController {

    private final PlatformService platformService;

    public PlatformAuditController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public List<PlatformAuditEntry> globalAudit() {
        return platformService.auditGlobal();
    }
}
