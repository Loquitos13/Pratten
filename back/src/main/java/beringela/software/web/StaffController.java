package beringela.software.web;

import beringela.software.dto.StaffDtos.StaffRequest;
import beringela.software.dto.StaffDtos.StaffResponse;
import beringela.software.service.StaffService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffResponse> list() {
        return staffService.findAll().stream().map(StaffResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable UUID id) {
        return StaffResponse.from(staffService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@Valid @RequestBody StaffRequest request) {
        return StaffResponse.from(staffService.create(request));
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable UUID id, @Valid @RequestBody StaffRequest request) {
        return StaffResponse.from(staffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        staffService.delete(id);
    }
}
