CREATE TABLE IF NOT EXISTS courses.course_attendance (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    course_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(255),
    marked_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_course_attendance_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT uq_course_attendance_user_date UNIQUE (course_id, user_id, attendance_date)
);

CREATE INDEX IF NOT EXISTS idx_course_attendance_course_date
    ON courses.course_attendance(course_id, attendance_date);
