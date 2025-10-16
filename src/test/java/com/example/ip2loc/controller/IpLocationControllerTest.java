package com.example.ip2loc.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ip2loc.model.LocationResponse;
import com.example.ip2loc.service.Ip2LocationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IpLocationController.class)
class IpLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Ip2LocationService locationService;

    @Test
    @DisplayName("当提供有效 IP 时返回定位信息")
    void shouldReturnLocationWhenIpProvided() throws Exception {
        LocationResponse response = new LocationResponse(
                "8.8.8.8", "US", "United States", "California", "Mountain View", "94043",
                37.386, -122.0838, "UTC-07:00", "Google LLC");
        when(locationService.lookup("8.8.8.8")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/location").param("ip", "8.8.8.8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ip").value("8.8.8.8"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.countryName").value("United States"));
    }

    @Test
    @DisplayName("当 IP 参数无效时返回 400")
    void shouldReturnBadRequestForInvalidIp() throws Exception {
        mockMvc.perform(get("/api/location").param("ip", "not-an-ip"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid IP address"));
    }

    @Test
    @DisplayName("未提供 IP 时使用远程地址查询")
    void shouldUseRemoteAddressWhenIpMissing() throws Exception {
        LocationResponse response = new LocationResponse(
                "1.2.3.4", "CN", "China", null, null, null, null, null, null, null);
        when(locationService.lookup("1.2.3.4")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/location").with(request -> {
                    request.setRemoteAddr("1.2.3.4");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("1.2.3.4"));
    }

    @Test
    @DisplayName("当数据库没有命中时返回 404")
    void shouldReturnNotFoundWhenLookupEmpty() throws Exception {
        when(locationService.lookup(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/location").param("ip", "203.0.113.1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Location not found"));
    }
}
