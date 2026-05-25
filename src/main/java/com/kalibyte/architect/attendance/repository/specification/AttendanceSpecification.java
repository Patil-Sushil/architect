package com.kalibyte.architect.attendance.repository.specification;

import com.kalibyte.architect.attendance.entity.Attendance;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendanceSpecification {

    public static Specification<Attendance> filterRecords(UUID userId, LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            // Join fetch User to avoid LazyInitializationException and N+1
            // Fetches are only allowed for data queries, not for count queries
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
