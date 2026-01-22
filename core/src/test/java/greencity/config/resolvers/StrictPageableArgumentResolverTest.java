package greencity.config.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.exception.handler.CustomExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StrictPageableArgumentResolverTest {
    private static final String PATH = "/test/pageable";

    private MockMvc mockMvc;

    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestPageableController())
            .setCustomArgumentResolvers(new StrictPageableArgumentResolver(1, 100))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setControllerAdvice(new CustomExceptionHandler(errorAttributes))
            .build();
    }

    @Test
    void get_shouldReturnBadRequest_whenPageIsNotInteger() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "abc")
            .param("size", "10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnBadRequest_whenSizeIsNotInteger() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "0")
            .param("size", "abc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnBadRequest_whenPageIsNegative() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "-1")
            .param("size", "10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnBadRequest_whenSizeIsLessThanMin() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "0")
            .param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnBadRequest_whenSizeIsGreaterThanMax() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "0")
            .param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnOkAndResolvedPageable_whenValidPageAndSizeProvided() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "2")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.pageNumber", is(2)))
            .andExpect(jsonPath("$.pageSize", is(10)));
    }

    @Test
    void get_shouldReturnOkAndResolvedSort_whenValidSortProvided() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "0")
            .param("size", "5")
            .param("sort", "id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sortProperty", is("id")))
            .andExpect(jsonPath("$.sortDirection", is("DESC")));
    }

    @Test
    void get_shouldReturnBadRequest_whenPageIsEmptyString() throws Exception {
        mockMvc.perform(get(PATH)
            .param("page", "")
            .param("size", "10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturnOk_whenPageAndSizeNotProvided() throws Exception {
        mockMvc.perform(get(PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber", is(0)));
    }

    @RestController
    @RequestMapping("/test")
    static class TestPageableController {
        @GetMapping("/pageable")
        public PageableEchoResponse get(Pageable pageable) {
            Sort.Order firstOrder = pageable.getSort().stream().findFirst().orElse(null);
            return new PageableEchoResponse(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                firstOrder != null ? firstOrder.getProperty() : null,
                firstOrder != null ? firstOrder.getDirection().name() : null);
        }
    }

    record PageableEchoResponse(
        int pageNumber,
        int pageSize,
        String sortProperty,
        String sortDirection) {
    }
}
