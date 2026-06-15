package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookedDateRangeDTO {
    private LocalDate checkIn;
    private LocalDate checkOut;
}
