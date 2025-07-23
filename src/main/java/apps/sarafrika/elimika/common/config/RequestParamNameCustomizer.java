package apps.sarafrika.elimika.common.config;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

@Component
public class RequestParamNameCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getParameters() != null) {
            MethodParameter[] methodParams = handlerMethod.getMethodParameters();

            for (MethodParameter methodParam : methodParams) {
                if (Pageable.class.isAssignableFrom(methodParam.getParameterType())) {
                    continue;
                }

                RequestParam requestParam = methodParam.getParameterAnnotation(RequestParam.class);
                if (requestParam != null) {

                    boolean hasExplicitName = !requestParam.name().isEmpty() ||
                            !requestParam.value().isEmpty();

                    if (!hasExplicitName) {
                        String variableName = methodParam.getParameterName();
                        operation.getParameters().stream()
                                .filter(param -> "query".equals(param.getIn()))
                                .filter(param -> {
                                    assert variableName != null;
                                    return variableName.equals(param.getName());
                                })
                                .findFirst()
                                .ifPresent(param -> param.setName(""));
                    }
                }
            }
        }
        return operation;
    }
}