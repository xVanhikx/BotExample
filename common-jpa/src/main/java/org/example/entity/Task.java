package org.example.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private boolean completed;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    AppUser user;

    public String isCompleted() {
        if (completed) {
            return "Выполнено";
        } else {
            return "Еще в процессе";
        }
    }
}
