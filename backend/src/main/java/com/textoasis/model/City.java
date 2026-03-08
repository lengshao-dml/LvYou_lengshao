package com.textoasis.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = "features")
@EqualsAndHashCode(exclude = "features")
@Entity
@Table(name = "cities")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String province;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "hotness_score")
    private Integer hotnessScore;

    private String pinyin;

    private String abbr;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CityFeature> features;

    @CreationTimestamp
    @Column(updatable = false, name = "create_time")
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Date updateTime;
}
