package com.textoasis.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Data
@Entity
@Table(name = "user_search_history")
public class UserSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 允许为null，因为匿名用户也可能搜索
    private User user;

    @Column(nullable = false)
    private String keyword;

    @CreationTimestamp
    @Column(updatable = false, name = "search_time")
    private Date searchTime;
}
