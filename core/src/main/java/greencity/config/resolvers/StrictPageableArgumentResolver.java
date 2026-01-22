package greencity.config.resolvers;

import greencity.exception.exceptions.BadRequestException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class StrictPageableArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String PAGE = "page";
    private static final String SIZE = "size";

    private final int minSize;
    private final int maxSize;

    private final PageableHandlerMethodArgumentResolver delegate;

    public StrictPageableArgumentResolver(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.delegate = new PageableHandlerMethodArgumentResolver(new SortHandlerMethodArgumentResolver());

        this.delegate.setPageParameterName(PAGE);
        this.delegate.setSizeParameterName(SIZE);
        this.delegate.setOneIndexedParameters(false);
        this.delegate.setMaxPageSize(maxSize);
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return Pageable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
        @NonNull MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        @NonNull NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory) throws Exception {
        validatePageAndSize(webRequest);
        return delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    private void validatePageAndSize(NativeWebRequest webRequest) {
        validateIntIfPresent(webRequest, PAGE);
        validateIntIfPresent(webRequest, SIZE);

        String pageStr = webRequest.getParameter(PAGE);
        if (pageStr != null) {
            int page = Integer.parseInt(pageStr);
            if (page < 0) {
                throw new BadRequestException("Invalid 'page' parameter. Value must be greater than or equal to 0.");
            }
        }

        String sizeStr = webRequest.getParameter(SIZE);
        if (sizeStr != null) {
            int size = Integer.parseInt(sizeStr);
            if (size < minSize || size > maxSize) {
                throw new BadRequestException("Invalid 'size' parameter. Value must be between %d and %d."
                    .formatted(minSize, maxSize));
            }
        }
    }

    private void validateIntIfPresent(NativeWebRequest webRequest, String name) {
        String value = webRequest.getParameter(name);
        if (value == null) {
            return;
        }
        if (!value.matches("^-?\\d+$")) {
            throw new BadRequestException("Invalid '%s' parameter. Value must be an integer.".formatted(name));
        }
    }
}