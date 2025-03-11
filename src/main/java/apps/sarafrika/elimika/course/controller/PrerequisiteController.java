package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.request.*;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteResponseDTO;
import apps.sarafrika.elimika.course.service.PrerequisiteService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = PrerequisiteController.ROOT_PATH)
class PrerequisiteController {

    protected static final String ROOT_PATH = "api/v1/prerequisite";
    private static final String ID_PATH = "{prerequisiteId}";
    private static final String GROUP_PATH = "group";
    private static final String GROUP_ID_PATH = "/{prerequisiteGroupId}";

    private final PrerequisiteService prerequisiteService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<PrerequisiteResponseDTO> findPrerequisites(PrerequisiteRequestDTO prerequisiteRequestDTO, Pageable pageable) {

        return prerequisiteService.findAllPrerequisites(prerequisiteRequestDTO, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createPrerequisite(@RequestBody CreatePrerequisiteRequestDTO createPrerequisiteRequestDTO) {

        return prerequisiteService.createPrerequisite(createPrerequisiteRequestDTO);
    }

    @PostMapping(GROUP_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createPrerequisiteGroup(@RequestBody CreatePrerequisiteGroupRequestDTO createPrerequisiteGroupRequestDTO) {

        return prerequisiteService.createPrerequisiteGroup(createPrerequisiteGroupRequestDTO);
    }

    @PutMapping(ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updatePrerequisite(@RequestBody UpdatePrerequisiteRequestDTO updatePrerequisiteRequestDTO, @PathVariable Long prerequisiteId) {

        return prerequisiteService.updatePrerequisite(updatePrerequisiteRequestDTO, prerequisiteId);
    }

    @DeleteMapping(ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePrerequisite(@PathVariable Long prerequisiteId) {

        prerequisiteService.deletePrerequisite(prerequisiteId);
    }

    @PutMapping(GROUP_PATH + GROUP_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updatePrerequisiteGroup(@RequestBody UpdatePrerequisiteGroupRequestDTO updatePrerequisiteGroupRequestDTO, @PathVariable Long prerequisiteGroupId) {

        return prerequisiteService.updatePrerequisiteGroup(updatePrerequisiteGroupRequestDTO, prerequisiteGroupId);
    }

    @DeleteMapping(GROUP_PATH + GROUP_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePrerequisiteGroup(@PathVariable Long prerequisiteGroupId) {

        prerequisiteService.deletePrerequisiteGroup(prerequisiteGroupId);
    }

}
