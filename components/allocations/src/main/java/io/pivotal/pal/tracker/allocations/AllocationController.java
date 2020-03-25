package io.pivotal.pal.tracker.allocations;

import io.pivotal.pal.tracker.allocations.data.AllocationDataGateway;
import io.pivotal.pal.tracker.allocations.data.AllocationFields;
import io.pivotal.pal.tracker.allocations.data.AllocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static io.pivotal.pal.tracker.allocations.AllocationInfo.allocationInfoBuilder;
import static io.pivotal.pal.tracker.allocations.data.AllocationFields.allocationFieldsBuilder;
import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/allocations")
public class AllocationController {
    private static final Logger log = LoggerFactory.getLogger( AllocationController.class );
    private final AllocationDataGateway gateway;
    private final ProjectClient client;

    public AllocationController(AllocationDataGateway gateway, ProjectClient client) {
        this.gateway = gateway;
        this.client = client;
    }


    @PostMapping
    public ResponseEntity<AllocationInfo> create(@RequestBody AllocationForm form) {

        if (projectIsActive(form.projectId)) {
            AllocationRecord record = gateway.create(formToFields(form));
            return new ResponseEntity<>(present(record), HttpStatus.CREATED);
        }

        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @GetMapping
    public List<AllocationInfo> list(@RequestParam long projectId) {
        return gateway.findAllByProjectId(projectId)
            .stream()
            .map(this::present)
            .collect(toList());
    }


    private boolean projectIsActive(long projectId) {
        ProjectInfo project = client.getProject(projectId);
        if (project == null) {
            log.warn("No project for ProjectId {}.", projectId);
            return false;
        }

        if (project.active) {
            log.warn("ProjectId: {} is active.", projectId);
        } else {
            log.warn("ProjectId {} is not active.", projectId);
        }

        return project.active;
    }

    private AllocationFields formToFields(AllocationForm form) {
        return allocationFieldsBuilder()
            .projectId(form.projectId)
            .userId(form.userId)
            .firstDay(LocalDate.parse(form.firstDay))
            .lastDay(LocalDate.parse(form.lastDay))
            .build();
    }

    private AllocationInfo present(AllocationRecord record) {
        return allocationInfoBuilder()
            .id(record.id)
            .projectId(record.projectId)
            .userId(record.userId)
            .firstDay(record.firstDay.toString())
            .lastDay(record.lastDay.toString())
            .info("allocation info")
            .build();
    }
}
