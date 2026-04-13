package com.shuran.art.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuran.art.entity.Course;
import com.shuran.art.mapper.CourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRS Domain - CourseController Integration Tests
 *
 * Tests the public course API endpoints (GET /api/course/list, GET /api/course/{id})
 * with a real H2 database and full Spring Boot context.
 *
 * Test skeleton source: CRS-test-detail.md Section 3.1 ~ 3.5, 3.10
 * L0 trace: EARS-CRS-001 ~ EARS-CRS-003, AC-CRS-001 ~ AC-CRS-003
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean course table before each test for isolation
        courseMapper.delete(null);
    }

    /**
     * Helper: insert a course into the database.
     */
    private Course insertCourse(String name, String category, int price,
                                String duration, String suitableFor,
                                int sortOrder, int status) {
        Course course = new Course();
        course.setName(name);
        course.setCategory(category);
        course.setPrice(price);
        course.setDuration(duration);
        course.setSuitableFor(suitableFor);
        course.setSortOrder(sortOrder);
        course.setStatus(status);
        courseMapper.insert(course);
        return course;
    }

    // ========================================================================
    // TP-CRS-007: GET /api/course/list - course list with data
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-007: GET /api/course/list - course list with data")
    class CourseListWithDataTests {

        /**
         * TC: TP-CRS-007
         * L0 trace: EARS-CRS-001, AC-CRS-001
         * L2 trace: CRS-test-detail.md Section 3.1
         *
         * Verifies: returns only active courses (status=1), ordered by sortOrder asc.
         */
        @Test
        @DisplayName("GET /api/course/list - returns active courses sorted by sortOrder")
        void getCourseList_withData_returnsActiveSortedCourses() throws Exception {
            // Arrange: 3 active courses + 1 inactive
            insertCourse("素描基础班", "素描", 2000, "3个月", "零基础学员", 1, 1);
            insertCourse("水彩提高班", "水彩", 3000, "2个月", "有基础学员", 2, 1);
            insertCourse("油画大师班", "油画", 5000, "4个月", "有素描基础", 3, 1);
            insertCourse("已下架课程", "国画", 1000, "1个月", "所有人", 0, 0);

            // Act & Assert
            mockMvc.perform(get("/api/course/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].name", is("素描基础班")))
                    .andExpect(jsonPath("$.data[1].name", is("水彩提高班")))
                    .andExpect(jsonPath("$.data[2].name", is("油画大师班")));
        }
    }

    // ========================================================================
    // TP-CRS-008: GET /api/course/list - empty list
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-008: GET /api/course/list - empty list")
    class EmptyCourseListTests {

        /**
         * TC: TP-CRS-008
         * L0 trace: AC-CRS-001, B4
         * L2 trace: CRS-test-detail.md Section 3.2
         *
         * Verifies: when all courses are inactive, returns empty array.
         */
        @Test
        @DisplayName("GET /api/course/list - all inactive - returns empty array")
        void getCourseList_allInactive_returnsEmptyArray() throws Exception {
            // Arrange: only inactive course
            insertCourse("唯一课程", "素描", 1000, "1个月", "所有人", 0, 0);

            // Act & Assert
            mockMvc.perform(get("/api/course/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // ========================================================================
    // TP-CRS-009: GET /api/course/list?category - category filter
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-009: GET /api/course/list?category - category filter")
    class CategoryFilterTests {

        /**
         * TC: TP-CRS-009
         * L0 trace: EARS-CRS-002, AC-CRS-002
         * L2 trace: CRS-test-detail.md Section 3.3
         *
         * Verifies: category filter returns only matching courses.
         */
        @Test
        @DisplayName("GET /api/course/list?category=素描 - returns only sketch courses")
        void getCourseList_categoryFilter_returnsMatchingCourses() throws Exception {
            // Arrange
            insertCourse("素描1", "素描", 2000, "3个月", "零基础", 1, 1);
            insertCourse("素描2", "素描", 2500, "2个月", "初学者", 2, 1);
            insertCourse("水彩1", "水彩", 3000, "2个月", "有基础", 3, 1);

            // Act & Assert
            mockMvc.perform(get("/api/course/list").param("category", "素描"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].category", is("素描")))
                    .andExpect(jsonPath("$.data[1].category", is("素描")));
        }

        /**
         * TC: TP-CRS-009 (empty category = all)
         * L0 trace: EARS-CRS-002, AC-CRS-002
         * L2 trace: CRS-test-detail.md Section 3.3
         */
        @Test
        @DisplayName("GET /api/course/list?category= - empty category returns all active")
        void getCourseList_emptyCategoryFilter_returnsAll() throws Exception {
            // Arrange
            insertCourse("素描1", "素描", 2000, "3个月", "零基础", 1, 1);
            insertCourse("水彩1", "水彩", 3000, "2个月", "有基础", 2, 1);

            // Act & Assert
            mockMvc.perform(get("/api/course/list").param("category", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        /**
         * TC: TP-CRS-020 (non-existent category)
         * L0 trace: B5
         * L2 trace: CRS-test-detail.md Section 3.3
         */
        @Test
        @DisplayName("GET /api/course/list?category=不存在的类别 - returns empty")
        void getCourseList_nonExistentCategory_returnsEmpty() throws Exception {
            // Arrange
            insertCourse("素描1", "素描", 2000, "3个月", "零基础", 1, 1);

            // Act & Assert
            mockMvc.perform(get("/api/course/list").param("category", "不存在的类别"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // ========================================================================
    // TP-CRS-010: GET /api/course/{id} - course exists
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-010: GET /api/course/{id} - course exists")
    class CourseDetailExistsTests {

        /**
         * TC: TP-CRS-010
         * L0 trace: EARS-CRS-003, AC-CRS-003
         * L2 trace: CRS-test-detail.md Section 3.4
         *
         * Verifies: returns the course details when id exists.
         */
        @Test
        @DisplayName("GET /api/course/{id} - course exists - returns course details")
        void getCourse_courseExists_returnsCourseDetails() throws Exception {
            // Arrange
            Course course = insertCourse("素描基础班", "素描", 2000, "3个月",
                    "零基础学员", 1, 1);

            // Act & Assert
            mockMvc.perform(get("/api/course/" + course.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data.name", is("素描基础班")))
                    .andExpect(jsonPath("$.data.price", is(2000)))
                    .andExpect(jsonPath("$.data.duration", is("3个月")))
                    .andExpect(jsonPath("$.data.suitableFor", is("零基础学员")));
        }
    }

    // ========================================================================
    // TP-CRS-011: GET /api/course/{id} - course not found
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-011: GET /api/course/{id} - course not found")
    class CourseDetailNotFoundTests {

        /**
         * TC: TP-CRS-011
         * L0 trace: EARS-CRS-003
         * L2 trace: CRS-test-detail.md Section 3.5
         *
         * Verifies: returns data=null when course id does not exist.
         */
        @Test
        @DisplayName("GET /api/course/999 - course not found - data is null")
        void getCourse_courseNotFound_dataIsNull() throws Exception {
            // Arrange: no course with id=999

            // Act & Assert
            mockMvc.perform(get("/api/course/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(0)))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // ========================================================================
    // TP-CRS-018: GET /api/course/list - sort order correctness
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-018: GET /api/course/list - sort order correctness")
    class SortOrderTests {

        /**
         * TC: TP-CRS-018
         * L0 trace: EARS-CRS-001
         * L2 trace: CRS-test-detail.md Section 3 (sorting)
         */
        @Test
        @DisplayName("GET /api/course/list - courses sorted by sortOrder ascending")
        void getCourseList_sortedBySortOrderAsc() throws Exception {
            // Arrange: insert in reverse order
            insertCourse("油画", "油画", 5000, "4个月", "有基础", 3, 1);
            insertCourse("素描", "素描", 2000, "3个月", "零基础", 1, 1);
            insertCourse("水彩", "水彩", 3000, "2个月", "有基础", 2, 1);

            // Act & Assert
            mockMvc.perform(get("/api/course/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name", is("素描")))
                    .andExpect(jsonPath("$.data[1].name", is("水彩")))
                    .andExpect(jsonPath("$.data[2].name", is("油画")));
        }
    }
}
