package apps.sarafrika.elimika.shared.web;

import apps.sarafrika.elimika.shared.utils.RemovedTextOperators;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects a read whose query string uses a removed text operator ({@code name_like},
 * {@code title_startswith}, ...) before it reaches the controller.
 * <p>
 * Endpoints that bind their query string to a filter map already refuse these keys in the
 * specification builder. Many listings bind only {@code q} and a {@code Pageable}, though, and there
 * an unknown key was simply never read: {@code GET /api/v1/courses?name_like=x} answered 200 with the
 * filter silently ignored. Checking here gives every GET under {@code /api} the same 400 and message.
 * The exception goes through the global exception handler like any other 400.
 */
public class RemovedTextOperatorInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            RemovedTextOperators.rejectAny(request.getParameterMap().keySet());
        }
        return true;
    }
}
