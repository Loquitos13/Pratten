package beringela.software.web;

import beringela.software.dto.TableDtos.TableAssignmentRequest;
import beringela.software.dto.TableDtos.TableRequest;
import beringela.software.dto.TableDtos.TableResponse;
import beringela.software.dto.TableDtos.TableStatusRequest;
import beringela.software.service.TableService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public List<TableResponse> list(@RequestParam(name = "waiterId", required = false) UUID waiterId) {
        var tables = waiterId != null ? tableService.findByWaiter(waiterId) : tableService.findAll();
        return tables.stream().map(TableResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TableResponse get(@PathVariable UUID id) {
        return TableResponse.from(tableService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse create(@Valid @RequestBody TableRequest request) {
        return TableResponse.from(tableService.create(request));
    }

    @PutMapping("/{id}")
    public TableResponse update(@PathVariable UUID id, @Valid @RequestBody TableRequest request) {
        return TableResponse.from(tableService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public TableResponse changeStatus(@PathVariable UUID id,
            @Valid @RequestBody TableStatusRequest request) {
        return TableResponse.from(tableService.changeStatus(id, request.status()));
    }

    @PatchMapping("/{id}/assignment")
    public TableResponse assignWaiter(@PathVariable UUID id,
            @Valid @RequestBody TableAssignmentRequest request) {
        return TableResponse.from(tableService.assignWaiter(id, request.waiterId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tableService.delete(id);
    }
}
