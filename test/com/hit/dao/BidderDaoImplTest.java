package com.hit.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hit.beans.BidderBean;
import com.hit.utility.DBUtil;
import com.hit.utility.IDUtil;

/**
 * Test class for BidderDaoImpl
 * Tests all bidding-related operations including bid creation, acceptance, rejection,
 * and retrieval of bid information
 */
@ExtendWith(MockitoExtension.class)
public class BidderDaoImplTest {

    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private TenderDao mockTenderDao;

    private Connection mockConnection;
    private BidderDaoImpl bidderDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    /**
     * Sets up the test environment before each test
     * Initializes mocks and common behavior
     */
    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        bidderDao = new BidderDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        
        // Configure default mock behaviors
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    /**
     * Tests bid acceptance when project is already assigned
     */
    @Test
    void acceptBid_WhenProjectAlreadyAssigned_ShouldReturnAppropriateMessage() throws SQLException {
        // Arrange
        String applicationId = "APP123";
        String tenderId = "TENDER123";
        String vendorId = "VENDOR123";

        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        // Act
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);

        // Assert
        assertAll(
            () -> assertEquals("Project Already Assigned", result, 
                "Should return project already assigned message"),
            () -> verify(mockPreparedStatement).setString(1, tenderId)
        );
    }

    /**
     * Tests successful bid acceptance
     */
    @Test
    void acceptBid_WhenSuccessful_ShouldAcceptBid() throws SQLException {
        // Arrange
        String applicationId = "APP123";
        String tenderId = "TENDER123";
        String vendorId = "VENDOR123";

        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);

        // Assert
        assertTrue(result.contains("Bid Has Been Accepted Successfully!"),
            "Should return success message for bid acceptance");
    }

    /**
     * Tests successful bid rejection
     */
    @Test
    void rejectBid_WhenSuccessful_ShouldRejectBid() throws SQLException {
        // Arrange
        String applicationId = "APP123";
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = bidderDao.rejectBid(applicationId);

        // Assert
        assertEquals("Bid Has Been Rejected Successfully!", result,
            "Should return success message for bid rejection");
    }

    /**
     * Tests successful bid creation
     */
    @Test
    void bidTender_WhenSuccessful_ShouldCreateBid() throws SQLException {
        // Arrange
        String tenderId = "TENDER123";
        String vendorId = "VENDOR123";
        String bidAmount = "1000";
        String bidDeadline = new Date().toString();
        MockedStatic<IDUtil> idUtilMocked = null;

        try {
            // Mock ID generation
            idUtilMocked = mockStatic(IDUtil.class);
            idUtilMocked.when(IDUtil::generateBidderId).thenReturn("BID123");
            
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            String result = bidderDao.bidTender(tenderId, vendorId, bidAmount, bidDeadline);

            // Assert
            assertEquals("You have successfully Bid for the tender", result,
                "Should return success message for bid creation");
        } finally {
            if (idUtilMocked != null) {
                idUtilMocked.close();
            }
        }
    }

    /**
     * Tests retrieval of all bids for a specific tender
     */
    @Test
    void getAllBidsOfaTender_ShouldReturnBidsList() throws SQLException {
        // Arrange
        String tenderId = "TENDER123";
        setupBidResultSetMock();

        // Act
        List<BidderBean> result = bidderDao.getAllBidsOfaTender(tenderId);

        // Assert
        assertBidResults(result);
    }

    /**
     * Tests retrieval of all bids for a specific vendor
     */
    @Test
    void getAllBidsOfaVendor_ShouldReturnBidsList() throws SQLException {
        // Arrange
        String vendorId = "VENDOR123";
        setupBidResultSetMock();

        // Act
        List<BidderBean> result = bidderDao.getAllBidsOfaVendor(vendorId);

        // Assert
        assertBidResults(result);
        assertEquals(vendorId, result.get(0).getVendorId(),
            "Vendor ID should match the requested ID");
    }

    /**
     * Tests error handling during bid acceptance
     */
    @Test
    void acceptBid_WhenSQLException_ShouldReturnErrorMessage() throws SQLException {
        // Arrange
        String applicationId = "APP123";
        String tenderId = "TENDER123";
        String vendorId = "VENDOR123";

        when(mockConnection.prepareStatement(anyString()))
            .thenThrow(new SQLException("Database error"));

        // Act
        String result = bidderDao.acceptBid(applicationId, tenderId, vendorId);

        // Assert
        assertAll(
            () -> assertTrue(result.contains("Bid Acceptance Failed"),
                "Should indicate bid acceptance failure"),
            () -> assertTrue(result.contains("Error:"),
                "Should include error message")
        );
    }

    /**
     * Helper method to set up common ResultSet mock behavior for bid queries
     */
    private void setupBidResultSetMock() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("bidamount")).thenReturn(1000);
        when(mockResultSet.getDate("deadline"))
            .thenReturn(new java.sql.Date(System.currentTimeMillis()));
        when(mockResultSet.getString("bid")).thenReturn("BID123");
        when(mockResultSet.getString("status")).thenReturn("Pending");
        when(mockResultSet.getString("tid")).thenReturn("TENDER123");
        when(mockResultSet.getString("vid")).thenReturn("VENDOR123");
    }

    /**
     * Helper method to assert common bid result expectations
     */
    private void assertBidResults(List<BidderBean> result) {
        assertAll(
            () -> assertNotNull(result, "Result should not be null"),
            () -> assertEquals(1, result.size(), "Should return exactly one bid"),
            () -> assertEquals("BID123", result.get(0).getBidId(), "Bid ID should match"),
            () -> assertEquals("Pending", result.get(0).getBidStatus(), "Status should be pending")
        );
    }

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
