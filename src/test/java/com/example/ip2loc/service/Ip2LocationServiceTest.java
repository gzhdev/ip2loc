package com.example.ip2loc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ip2loc.config.Ip2LocationProperties;
import com.example.ip2loc.model.LocationResponse;
import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class Ip2LocationServiceTest {

    private Ip2LocationService service;
    private Ip2LocationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new Ip2LocationProperties();
        properties.setToken(null); // disable download in unit test
        properties.setDatabasePath(Path.of("build", "test", "db.bin"));
        service = new Ip2LocationService(properties);
    }

    @Test
    @DisplayName("在数据库可用时成功返回定位信息")
    void shouldReturnLocationWhenDatabasePresent() throws Exception {
        IP2Location ip2Location = mock(IP2Location.class);
        IPResult result = mock(IPResult.class);
        when(result.getStatus()).thenReturn("OK");
        when(result.getCountryShort()).thenReturn("US");
        when(result.getCountryLong()).thenReturn("United States");
        when(result.getLatitude()).thenReturn(37.386f);
        when(result.getLongitude()).thenReturn(-122.0838f);
        when(result.getISP()).thenReturn("Google LLC");
        when(ip2Location.IPQuery("8.8.8.8")).thenReturn(result);

        ReflectionTestUtils.setField(service, "database", ip2Location);

        Optional<LocationResponse> response = service.lookup("8.8.8.8");

        assertThat(response).isPresent();
        assertThat(response.get().countryCode()).isEqualTo("US");
        assertThat(response.get().latitude()).isCloseTo(37.386, within(0.001));
    }

    @Test
    @DisplayName("当数据库不可用或查询失败时返回空结果")
    void shouldReturnEmptyWhenDatabaseMissing() {
        Optional<LocationResponse> response = service.lookup("8.8.4.4");
        assertThat(response).isEmpty();
    }
}
