package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;

@Entity
@Table(name = "activity_reminders")
@Data
public class ActivityReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ActivityRegistration registration;

    private boolean remind1Day = false;
    private boolean remind1Hour = false;


}
