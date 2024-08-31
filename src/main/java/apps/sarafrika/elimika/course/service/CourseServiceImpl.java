package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public CourseResponseDTO findById(Long id) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePageableDTO<CourseResponseDTO> findAll(Pageable pageable) {

        Page<CourseResponseDTO> coursesPage = courseRepository.findAll(pageable)
                .stream()
                .map(CourseResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(coursesPage.getContent(), coursesPage.getNumber(), coursesPage.getSize(),
                coursesPage.getTotalPages(), coursesPage.getTotalElements());
    }

    @Override
    public void create(CreateCourseRequestDTO course) {

    }

    @Override
    public CourseResponseDTO update(Course course) {
        return null;
    }

    @Override
    public void delete(int id) {

    }


}
