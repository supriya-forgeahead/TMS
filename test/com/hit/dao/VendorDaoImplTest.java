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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hit.beans.VendorBean;
import com.hit.utility.DBUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendorDaoImplTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private VendorDaoImpl vendorDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    @BeforeEach
    void setUp() throws SQLException {
        vendorDao = new VendorDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        
        // Configure default mock behaviors
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock DBUtil.closeConnection to do nothing
        dbUtilMocked.when(() -> DBUtil.closeConnection(any(Connection.class))).thenAnswer(invocation -> null);
        dbUtilMocked.when(() -> DBUtil.closeConnection(any(PreparedStatement.class))).thenAnswer(invocation -> null);
        dbUtilMocked.when(() -> DBUtil.closeConnection(any(ResultSet.class))).thenAnswer(invocation -> null);
    }

    @Test
    void registerVendor_WhenNewVendor_ShouldReturnSuccess() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Test Vendor", "1234567890", 
            "test@test.com", "Test Address", "Test Company", "password123");
        
        when(mockResultSet.next()).thenReturn(false);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = vendorDao.registerVendor(vendor);

        // Assert
        assertTrue(result.contains("Registration Successful"));
    }

    @Test
    void registerVendor_WhenEmailExists_ShouldReturnDeclined() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Test Vendor", "1234567890", 
            "existing@test.com", "Test Address", "Test Company", "password123");
        
        when(mockResultSet.next()).thenReturn(true);

        // Act
        String result = vendorDao.registerVendor(vendor);

        // Assert
        assertTrue(result.contains("Registration Declined"));
    }

    @Test
    void getAllVendors_WhenVendorsExist_ShouldReturnList() throws SQLException {
        // Arrange
        when(mockResultSet.next()).thenReturn(true, true, false);
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

    @Test
    void validatePassword_WhenValid_ShouldReturnTrue() throws SQLException {
        // Arrange
        String vendorId = "V001";
        String password = "password123";
        
        when(mockResultSet.next()).thenReturn(true);

        // Act
        boolean result = vendorDao.validatePassword(vendorId, password);

        // Assert
        assertTrue(result);
    }

    @Test
    void updateProfile_WhenValidCredentials_ShouldReturnSuccess() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Updated Name", "9876543210", 
            "updated@test.com", "New Address", "New Company", "password123");
        
        // Mock validatePassword
        when(mockResultSet.next()).thenReturn(true);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = vendorDao.updateProfile(vendor);

        // Assert
        assertTrue(result.contains("Updated Successfully"));
    }

    @Test
    void updateProfile_WhenInvalidPassword_ShouldReturnFailure() throws SQLException {
        // Arrange
        VendorBean vendor = new VendorBean("V001", "Test Name", "1234567890", 
            "test@test.com", "Test Address", "Test Company", "wrongpassword");
        
        // Mock validatePassword to return false
        when(mockResultSet.next()).thenReturn(false);

        // Act
        String result = vendorDao.updateProfile(vendor);

        // Assert
        assertTrue(result.contains("Wrong Password"));
    }

    @Test
    void changePassword_WhenValidOldPassword_ShouldReturnSuccess() throws SQLException {
        // Arrange
        String vendorId = "V001";
        String oldPassword = "oldpass";
        String newPassword = "newpass";
        
        // Mock validatePassword
        when(mockResultSet.next()).thenReturn(true);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = vendorDao.changePassword(vendorId, oldPassword, newPassword);

        // Assert
        assertTrue(result.contains("Updated Successfully"));
    }

    @Test
    void getVendorDataById_WhenExists_ShouldReturnVendor() throws SQLException {
        // Arrange
        String vendorId = "V001";
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("vid")).thenReturn("V001");
        when(mockResultSet.getString("vname")).thenReturn("Test Vendor");
        when(mockResultSet.getString("vmob")).thenReturn("1234567890");
        when(mockResultSet.getString("vemail")).thenReturn("test@test.com");
        when(mockResultSet.getString("address")).thenReturn("Test Address");
        when(mockResultSet.getString("company")).thenReturn("Test Company");
        when(mockResultSet.getString("password")).thenReturn("password123");

        // Act
        VendorBean result = vendorDao.getVendorDataById(vendorId);

        // Assert
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("V001", result.getId()),
            () -> assertEquals("Test Vendor", result.getName()),
            () -> assertEquals("test@test.com", result.getEmail())
        );
    }

    @AfterEach
    void tearDown() {
        try {
            if (dbUtilMocked != null) {
                dbUtilMocked.close();
            }
            // Reset all mocks
            reset(mockConnection, mockPreparedStatement, mockResultSet);
        } catch (Exception e) {
            System.err.println("Error in tearDown: " + e.getMessage());
        }
    }
}
