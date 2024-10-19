package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteTypeRequestDTO;
import apps.sarafrika.elimika.course.service.PrerequisiteTypeService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(path = PrerequisiteTypeController.ROOT_PATH)
@RequiredArgsConstructor
public class PrerequisiteTypeController {

    protected static final String ROOT_PATH = "api/v1/prerequisite-types";

    private final PrerequisiteTypeService prerequisiteTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createPrerequisiteType(@RequestBody CreatePrerequisiteTypeRequestDTO createPrerequisiteTypeRequestDTO) {

        return prerequisiteTypeService.createPrerequisiteType(createPrerequisiteTypeRequestDTO);
    }
}
