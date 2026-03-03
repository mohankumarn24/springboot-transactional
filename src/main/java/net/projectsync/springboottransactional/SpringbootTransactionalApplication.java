package net.projectsync.springboottransactional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringbootTransactionalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootTransactionalApplication.class, args);
    }
}


/*
Endpoints:
http://localhost:8080/required
http://localhost:8080/requires-new
http://localhost:8080/nested
http://localhost:8080/supports
http://localhost:8080/mandatory
http://localhost:8080/never

id|name     |
--+---------+
1 |SUPPORTS |
2 |MANDATORY|
3 |NEVER    |
*/