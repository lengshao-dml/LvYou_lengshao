package com.textoasis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Data
@Entity
@Table(name = "user_recommend_log")
public class UserRecommendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 允许为null
    private User user;

    @Column
    private String interestTags; // e.g., "历史文化,美食文化"

    @Column
    private String distanceScope;

    @CreationTimestamp
    @Column(updatable = false, name = "request_time")
    private Date requestTime;
}
