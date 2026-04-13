package com.shuran.art.service;

import com.shuran.art.entity.Course;
import com.shuran.art.mapper.CourseMapper;
import com.shuran.art.mapper.AdminWhitelistMapper;
import com.shuran.art.mapper.StudioConfigMapper;
import com.shuran.art.mapper.TeacherMapper;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.PrizeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CRS Domain - AdminService Unit Tests
 *
 * Tests the course management business logic in AdminService:
 * saveCourse (new/edit/validation), deleteCourse (idempotent), updateCourseStatus.
 *
 * Test skeleton source: CRS-test-detail.md Section 2
 * L0 trace: EARS-CRS-005, AC-CRS-004
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceCourseTest {

    @Mock
    private AdminWhitelistMapper adminWhitelistMapper;
    @Mock
    private StudioConfigMapper studioConfigMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private PrizeMapper prizeMapper;
    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private AdminService adminService;

    // --- Helper: build a valid Course with all required fields ---
    private Course buildValidCourse() {
        Course course = new Course();
        course.setName("国画入门班");
        course.setCategory("国画");
        course.setPrice(2500);
        course.setDuration("2个月");
        course.setSuitableFor("零基础");
        return course;
    }

    // ========================================================================
    // TP-CRS-001: AdminService.saveCourse new-insert logic
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-001: saveCourse - new course (id=null -> insert)")
    class SaveCourseNewTests {

        /**
         * TC: TP-CRS-001
         * L0 trace: EARS-CRS-005, AC-CRS-004
         * L2 trace: CRS-test-detail.md Section 2.1
         *
         * Verifies: when Course.id is null, courseMapper.insert() is called
         * and courseMapper.updateById() is never called.
         */
        @Test
        @DisplayName("saveCourse - id is null - calls insert, never updateById")
        void saveCourse_idIsNull_callsInsert() {
            // Arrange
            Course course = buildValidCourse();
            course.setId(null);
            when(courseMapper.insert(any(Course.class))).thenReturn(1);

            // Act
            adminService.saveCourse(course);

            // Assert
            verify(courseMapper).insert(course);
            verify(courseMapper, never()).updateById(any());
        }

        /**
         * TC: TP-CRS-001 (supplementary - default values)
         * L0 trace: EARS-CRS-005
         * L2 trace: CRS-test-detail.md Section 2.1, V-011, V-012
         *
         * Verifies: when sortOrder and status are null, defaults are applied
         * (sortOrder=0, status=1).
         */
        @Test
        @DisplayName("saveCourse - null sortOrder/status - applies defaults (sortOrder=0, status=1)")
        void saveCourse_nullDefaults_appliesDefaults() {
            // Arrange
            Course course = buildValidCourse();
            course.setId(null);
            course.setSortOrder(null);
            course.setStatus(null);
            when(courseMapper.insert(any(Course.class))).thenReturn(1);

            // Act
            adminService.saveCourse(course);

            // Assert
            assertThat(course.getSortOrder()).isEqualTo(0);
            assertThat(course.getStatus()).isEqualTo(1);
            verify(courseMapper).insert(course);
        }
    }

    // ========================================================================
    // TP-CRS-002: AdminService.saveCourse edit-update logic
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-002: saveCourse - edit course (id != null -> updateById)")
    class SaveCourseEditTests {

        /**
         * TC: TP-CRS-002
         * L0 trace: EARS-CRS-005
         * L2 trace: CRS-test-detail.md Section 2.2
         *
         * Verifies: when Course.id is non-null, courseMapper.updateById() is called
         * and courseMapper.insert() is never called.
         */
        @Test
        @DisplayName("saveCourse - id has value - calls updateById, never insert")
        void saveCourse_idHasValue_callsUpdateById() {
            // Arrange
            Course course = buildValidCourse();
            course.setId(1L);
            course.setName("素描基础班（已更新）");
            course.setCategory("素描");
            course.setPrice(2200);
            course.setDuration("3个月");
            course.setSuitableFor("零基础学员");
            when(courseMapper.updateById(any(Course.class))).thenReturn(1);

            // Act
            adminService.saveCourse(course);

            // Assert
            verify(courseMapper).updateById(course);
            verify(courseMapper, never()).insert(any());
        }
    }

    // ========================================================================
    // TP-CRS-003: AdminService.saveCourse field validation
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-003: saveCourse - field validation")
    class SaveCourseValidationTests {

        /**
         * TC: TP-CRS-003 (name null)
         * L0 trace: EARS-CRS-005, V-004
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - name is null - throws RuntimeException")
        void saveCourse_nameNull_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setName(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程名称不能为空");
        }

        /**
         * TC: TP-CRS-003 (name blank)
         * L0 trace: EARS-CRS-005, V-004
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - name is blank - throws RuntimeException")
        void saveCourse_nameBlank_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setName("   ");

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程名称不能为空");
        }

        /**
         * TC: TP-CRS-003 (category null)
         * L0 trace: EARS-CRS-005, V-005
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - category is null - throws RuntimeException")
        void saveCourse_categoryNull_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setCategory(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程类别不能为空");
        }

        /**
         * TC: TP-CRS-003 (price negative)
         * L0 trace: EARS-CRS-005, V-006
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - price is negative - throws RuntimeException")
        void saveCourse_priceNegative_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setPrice(-100);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程价格不能为负数");
        }

        /**
         * TC: TP-CRS-003 (price null)
         * L0 trace: EARS-CRS-005, V-006
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - price is null - throws RuntimeException")
        void saveCourse_priceNull_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setPrice(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程价格不能为负数");
        }

        /**
         * TC: TP-CRS-003 (status invalid)
         * L0 trace: EARS-CRS-005, V-012
         * L2 trace: CRS-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("saveCourse - status is invalid (2) - throws RuntimeException")
        void saveCourse_statusInvalid_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setStatus(2);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("状态值无效");
        }

        /**
         * TC: TP-CRS-003 (duration null)
         * L0 trace: EARS-CRS-005, V-007
         * L2 trace: CRS-test-detail.md Section 2.3 (inferred from CV-004)
         */
        @Test
        @DisplayName("saveCourse - duration is null - throws RuntimeException")
        void saveCourse_durationNull_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setDuration(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程时长不能为空");
        }

        /**
         * TC: TP-CRS-003 (suitableFor null)
         * L0 trace: EARS-CRS-005, V-008
         * L2 trace: CRS-test-detail.md Section 2.3 (inferred from CV-005)
         */
        @Test
        @DisplayName("saveCourse - suitableFor is null - throws RuntimeException")
        void saveCourse_suitableForNull_throwsException() {
            // Arrange
            Course course = buildValidCourse();
            course.setSuitableFor(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.saveCourse(course))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("适合人群不能为空");
        }
    }

    // ========================================================================
    // TP-CRS-004: AdminService.deleteCourse idempotency
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-004: deleteCourse - idempotent behavior")
    class DeleteCourseTests {

        /**
         * TC: TP-CRS-004 (course exists)
         * L0 trace: EARS-CRS-005, B6
         * L2 trace: CRS-test-detail.md Section 2.4
         */
        @Test
        @DisplayName("deleteCourse - course exists - deleted normally")
        void deleteCourse_courseExists_deletedNormally() {
            // Arrange
            when(courseMapper.deleteById(1L)).thenReturn(1);

            // Act
            adminService.deleteCourse(1L);

            // Assert
            verify(courseMapper).deleteById(1L);
        }

        /**
         * TC: TP-CRS-004 (course does not exist)
         * L0 trace: EARS-CRS-005
         * L2 trace: CRS-test-detail.md Section 2.4
         */
        @Test
        @DisplayName("deleteCourse - course does not exist - no error (idempotent)")
        void deleteCourse_courseNotExist_noError() {
            // Arrange
            when(courseMapper.deleteById(999L)).thenReturn(0);

            // Act (should not throw)
            adminService.deleteCourse(999L);

            // Assert
            verify(courseMapper).deleteById(999L);
        }
    }

    // ========================================================================
    // TP-CRS-005: AdminService.updateCourseStatus normal flow
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-005: updateCourseStatus - normal flow")
    class UpdateCourseStatusTests {

        /**
         * TC: TP-CRS-005 (active to inactive)
         * L0 trace: EARS-CRS-005, AC-CRS-004
         * L2 trace: CRS-test-detail.md Section 2.5
         */
        @Test
        @DisplayName("updateCourseStatus - active to inactive - status updated to 0")
        void updateCourseStatus_activeToInactive_statusUpdated() {
            // Arrange
            Course existingCourse = new Course();
            existingCourse.setId(1L);
            existingCourse.setStatus(1);
            when(courseMapper.selectById(1L)).thenReturn(existingCourse);
            when(courseMapper.updateById(any())).thenReturn(1);

            // Act
            adminService.updateCourseStatus(1L, 0);

            // Assert
            verify(courseMapper).updateById(argThat(c -> c.getStatus() == 0));
        }

        /**
         * TC: TP-CRS-005 (inactive to active)
         * L0 trace: EARS-CRS-005
         * L2 trace: CRS-test-detail.md Section 2.5
         */
        @Test
        @DisplayName("updateCourseStatus - inactive to active - status updated to 1")
        void updateCourseStatus_inactiveToActive_statusUpdated() {
            // Arrange
            Course existingCourse = new Course();
            existingCourse.setId(1L);
            existingCourse.setStatus(0);
            when(courseMapper.selectById(1L)).thenReturn(existingCourse);
            when(courseMapper.updateById(any())).thenReturn(1);

            // Act
            adminService.updateCourseStatus(1L, 1);

            // Assert
            verify(courseMapper).updateById(argThat(c -> c.getStatus() == 1));
        }
    }

    // ========================================================================
    // TP-CRS-006: AdminService.updateCourseStatus course not found
    // ========================================================================
    @Nested
    @DisplayName("TP-CRS-006: updateCourseStatus - course not found")
    class UpdateCourseStatusNotFoundTests {

        /**
         * TC: TP-CRS-006
         * L0 trace: EARS-CRS-005
         * L2 trace: CRS-test-detail.md Section 2.6
         */
        @Test
        @DisplayName("updateCourseStatus - course not found - throws RuntimeException")
        void updateCourseStatus_courseNotFound_throwsException() {
            // Arrange
            when(courseMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> adminService.updateCourseStatus(999L, 0))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("课程不存在");
        }

        /**
         * TC: TP-CRS-006 (supplementary - null status)
         * L0 trace: EARS-CRS-005, V-015
         * L2 trace: CRS-test-detail.md Section 2.6
         */
        @Test
        @DisplayName("updateCourseStatus - status is null - throws RuntimeException")
        void updateCourseStatus_statusNull_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> adminService.updateCourseStatus(1L, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("状态值不能为空");
        }

        /**
         * TC: TP-CRS-006 (supplementary - invalid status)
         * L0 trace: EARS-CRS-005, V-015, CV-006
         * L2 trace: CRS-test-detail.md Section 2.6
         */
        @Test
        @DisplayName("updateCourseStatus - status is invalid (3) - throws RuntimeException")
        void updateCourseStatus_statusInvalid_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> adminService.updateCourseStatus(1L, 3))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("状态值无效");
        }
    }
}
