package com.textoasis.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString(exclude = {"city", "tag"})
@EqualsAndHashCode(exclude = {"city", "tag"})
@Entity
@Table(name = "city_features")
public class CityFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @OneToMany(mappedBy = "cityFeature", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attraction> attractions = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false, name = "create_time")
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Date updateTime;
}
