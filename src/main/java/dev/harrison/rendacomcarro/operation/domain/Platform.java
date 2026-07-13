package dev.harrison.rendacomcarro.operation.domain;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="platform")
public class Platform {
 @Id private UUID id;
 @Column(nullable=false, unique=true) private String code;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private boolean active;
 protected Platform() {}
 public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public boolean isActive(){return active;}
}
