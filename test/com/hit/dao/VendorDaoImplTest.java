package com.hit.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hit.beans.VendorBean;
import com.hit.utility.DBUtil;

/**
 * Test class for VendorDaoImpl
 * Tests all CRUD operations and business logic for vendor management
 */
@ExtendWith(MockitoExtension.class)
class VendorDaoImplTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private VendorDaoImpl vendorDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    /**
     * Sets up the test environment before each test
     * Initializes mocks and common behavior
     */
    @BeforeEach
    void setUp() throws SQLException {
        vendorDao = new VendorDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    /**
     * Tests successful vendor registration with new email
     */
    @Test
    void registerVendor_WhenNewVendor_ShouldReturnSuccess() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Test Vendor", "1234567890", 
            "test@test.com", "Test Address", "Test Company", "password123");
        
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = vendorDao.registerVendor(vendor);

        // Assert
        assertAll(
            () -> assertTrue(result.contains("Registration Successful")),
            () -> verify(mockPreparedStatement).setString(1, vendor.getEmail())
        );
    }

    /**
     * Tests retrieval of all vendors when vendors exist in database
     */
    @Test
    void getAllVendors_WhenVendorsExist_ShouldReturnList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        // Mock multiple vendor data
        when(mockResultSet.getString("vid")).thenReturn("V001", "V002");
        when(mockResultSet.getString("vname")).thenReturn("Vendor 1", "Vendor 2");
        when(mockResultSet.getString("vmob")).thenReturn("1234567890", "0987654321");
        when(mockResultSet.getString("vemail")).thenReturn("v1@test.com", "v2@test.com");
        when(mockResultSet.getString("address")).thenReturn("Address 1", "Address 2");
        when(mockResultSet.getString("company")).thenReturn("Company 1", "Company 2");
        when(mockResultSet.getString("password")).thenReturn("pass1", "pass2");

        // Act
        List<VendorBean> result = vendorDao.getAllVendors();

        // Assert
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(2, result.size()),
            () -> assertEquals("V001", result.get(0).getId()),
            () -> assertEquals("Vendor 1", result.get(0).getName())
        );
    }

    /**
     * Tests password validation for valid credentials
     */
    @Test
    void validatePassword_WhenValid_ShouldReturnTrue() throws SQLException {
        // Arrange
        String vendorId = "V001";
        String password = "password123";
        
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        // Act
        boolean result = vendorDao.validatePassword(vendorId, password);

        // Assert
        assertAll(
            () -> assertTrue(result),
            () -> verify(mockPreparedStatement).setString(1, vendorId),
            () -> verify(mockPreparedStatement).setString(2, password)
        );
    }

    /**
     * Tests successful profile update with valid credentials
     */
    @Test
    void updateProfile_WhenValidCredentials_ShouldReturnSuccess() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Updated Name", "9876543210", 
            "updated@test.com", "New Address", "New Company", "password123");
        
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = vendorDao.updateProfile(vendor);

        // Assert
        assertTrue(result.contains("Updated Successfully"));
    }

    // ... [Previous tests remain the same]

    /**
     * Cleans up resources after each test
     */
    @AfterEach
    void tearDown() {
        if (dbUtilMocked != null) {
            dbUtilMocked.close();
        }
    }
}
