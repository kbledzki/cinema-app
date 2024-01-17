package com.cinema.halls.ui;

import com.cinema.halls.application.HallService;
import com.cinema.halls.application.dto.CreateHallDto;
import com.cinema.halls.application.dto.HallDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HallController {

    private final HallService hallService;

    @PostMapping("/admin/halls")
    @SecurityRequirement(name = "basic")
    public ResponseEntity<Object> createHall(@RequestBody CreateHallDto dto) {
        log.info("Dto:{}", dto);
        hallService.createHall(dto);
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        log.info("Response:{}", responseEntity);
        return responseEntity;
    }

    @DeleteMapping("/admin/halls/{id}")
    @SecurityRequirement(name = "basic")
    public ResponseEntity<Object> deleteHall(@PathVariable Long id) {
        log.info("Hall id:{}", id);
        hallService.deleteHall(id);
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        log.info("Response:{}", responseEntity);
        return responseEntity;
    }

    @GetMapping("/admin/halls")
    @SecurityRequirement(name = "basic")
    public HallsResponse getAllHalls() {
        List<HallDto> halls =  hallService.getAllHalls();
        return new HallsResponse(halls);
    }
}
