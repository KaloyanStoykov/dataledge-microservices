package org.dataledge.datasourceservice.data.datasources;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.dataledge.datasourceservice.data.DataType;
import org.springframework.data.jpa.domain.Specification;

public class DataSourceSpecs {
    public static Specification<DataSource> search(int userId, String searchTerm) {
        return (root, query, cb) -> {
            // 1. Mandatory filter by User ID
            Predicate userPredicate = cb.equal(root.get("userId"), userId);

            // 2. If no search term, return just the user filter
            if (searchTerm == null || searchTerm.isBlank()) {
                return userPredicate;
            }

            String pattern = "%" + searchTerm.toLowerCase() + "%";

            // 3. Join with DataType to search by type.name (e.g., searching "API")
            // We use JoinType.INNER because nullable=false in your entity
            Join<DataSource, DataType> typeJoin = root.join("type");

            // 4. Combine search fields with OR
            Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("url")), pattern),
                    cb.like(cb.lower(typeJoin.get("name")), pattern)
            );

            // 5. Final query: WHERE userId = ? AND (name LIKE ... OR description LIKE ... etc)
            return cb.and(userPredicate, searchPredicate);
        };
    }
}