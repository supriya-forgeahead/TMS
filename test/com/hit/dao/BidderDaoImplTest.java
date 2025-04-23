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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hit.beans.BidderBean;
import com.hit.utility.DBUtil;
import com.hit.utility.IDUtil;

/**
 * Test class for BidderDaoImpl
 * Tests all bidding-related operations including bid creation, acceptance, rejection,
 * and retrieval of bid information
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @BeforeEach
    void setUp() throws SQLException {
        try {
            mockConnection = mock(Connection.class);
            bidderDao = new BidderDaoImpl();
            dbUtilMocked = mockStatic(DBUtil.class);
            
            dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        } catch (Exception e) {
            if (dbUtilMocked != null) {
                dbUtilMocked.close();
            }
            throw e;
        }
    }

    @Test
    void acceptBid_WhenProjectAlreadyAssigned_ShouldReturnAppropriateMessage() {
        try {
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
                () -> assertEquals("Project Already Assigned", result),
                () -> verify(mockPreparedStatement).setString(1, tenderId)
            );
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    void acceptBid_WhenSuccessful_ShouldAcceptBid() {
        try {
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
            assertTrue(result.contains("Bid Has Been Accepted Successfully!"));
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    void rejectBid_WhenSuccessful_ShouldRejectBid() {
        try {
            // Arrange
            String applicationId = "APP123";
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            String result = bidderDao.rejectBid(applicationId);

            // Assert
            assertEquals("Bid Has Been Rejected Successfully!", result);
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    void bidTender_WhenSuccessful_ShouldCreateBid() {
        MockedStatic<IDUtil> idUtilMocked = null;
        try {
            // Arrange
            String tenderId = "TENDER123";
            String vendorId = "VENDOR123";
            String bidAmount = "1000";
            String bidDeadline = new Date().toString();

            idUtilMocked = mockStatic(IDUtil.class);
            idUtilMocked.when(IDUtil::generateBidderId).thenReturn("BID123");
            
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            // Act
            String result = bidderDao.bidTender(tenderId, vendorId, bidAmount, bidDeadline);

            // Assert
            assertEquals("You have successfully Bid for the tender", result);
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        } finally {
            if (idUtilMocked != null) {
                idUtilMocked.close();
            }
        }
    }

    @Test
    void getAllBidsOfaTender_ShouldReturnBidsList() {
        try {
            // Arrange
            String tenderId = "TENDER123";
            setupBidResultSetMock();

            // Act
            List<BidderBean> result = bidderDao.getAllBidsOfaTender(tenderId);

            // Assert
            assertBidResults(result);
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    void getAllBidsOfaVendor_ShouldReturnBidsList() {
        try {
            // Arrange
            String vendorId = "VENDOR123";
            setupBidResultSetMock();

            // Act
            List<BidderBean> result = bidderDao.getAllBidsOfaVendor(vendorId);

            // Assert
            assertBidResults(result);
            assertEquals(vendorId, result.get(0).getVendorId());
        } catch (SQLException e) {
            fail("Should not throw SQLException: " + e.getMessage());
        }
    }

    @Test
    void acceptBid_WhenSQLException_ShouldReturnErrorMessage() {
        try {
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
                () -> assertTrue(result.contains("Bid Acceptance Failed")),
                () -> assertTrue(result.contains("Error:"))
            );
        } catch (SQLException e) {
            fail("Should handle SQLException properly: " + e.getMessage());
        }
    }

    private void setupBidResultSetMock() throws SQLException {
        try {
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getInt("bidamount")).thenReturn(1000);
            when(mockResultSet.getDate("deadline"))
                .thenReturn(new java.sql.Date(System.currentTimeMillis()));
            when(mockResultSet.getString("bid")).thenReturn("BID123");
            when(mockResultSet.getString("status")).thenReturn("Pending");
            when(mockResultSet.getString("tid")).thenReturn("TENDER123");
            when(mockResultSet.getString("vid")).thenReturn("VENDOR123");
        } catch (SQLException e) {
            throw new SQLException("Error setting up mock: " + e.getMessage());
        }
    }

    private void assertBidResults(List<BidderBean> result) {
        assertNotNull(result, "Result list should not be null");
        if (!result.isEmpty()) {
            assertAll(
                () -> assertEquals(1, result.size(), "Should return exactly one bid"),
                () -> assertNotNull(result.get(0), "Bid should not be null"),
                () -> assertEquals("BID123", result.get(0).getBidId(), "Bid ID should match"),
                () -> assertEquals("Pending", result.get(0).getBidStatus(), "Status should be pending")
            );
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (dbUtilMocked != null) {
                dbUtilMocked.close();
            }
            // Reset all mocks after each test
            reset(mockPreparedStatement, mockResultSet, mockTenderDao);
        } catch (Exception e) {
            System.err.println("Error in tearDown: " + e.getMessage());
        }
    }
}
