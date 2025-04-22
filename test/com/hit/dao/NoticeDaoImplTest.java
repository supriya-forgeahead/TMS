package com.hit.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.hit.beans.NoticeBean;
import com.hit.utility.DBUtil;

/**
 * Test class for NoticeDaoImpl
 * Tests CRUD operations for notice management including viewing, adding, and removing notices
 */
@ExtendWith(MockitoExtension.class)
public class NoticeDaoImplTest {

    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private Connection mockConnection;
    private NoticeDaoImpl noticeDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    /**
     * Sets up the test environment before each test
     * Initializes mocks and common behavior
     */
    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        noticeDao = new NoticeDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        
        // Configure default mock behaviors
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    /**
     * Tests retrieval of notices when notices exist in the database
     */
    @Test
    void viewAllNotice_WhenNoticesExist_ShouldReturnNoticeList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false); // Two notices
        
        // Mock notice data
        when(mockResultSet.getInt("id")).thenReturn(123, 124);
        when(mockResultSet.getString("title")).thenReturn("Test Notice 1", "Test Notice 2");
        when(mockResultSet.getString("info")).thenReturn("Info 1", "Info 2");

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertAll(
            () -> assertNotNull(result, "Result should not be null"),
            () -> assertEquals(2, result.size(), "Should return two notices"),
            // Verify first notice
            () -> assertNotice(result.get(0), 123, "Test Notice 1", "Info 1"),
            // Verify second notice
            () -> assertNotice(result.get(1), 124, "Test Notice 2", "Info 2")
        );
    }

    /**
     * Tests retrieval of notices when no notices exist
     */
    @Test
    void viewAllNotice_WhenNoNotices_ShouldReturnEmptyList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertAll(
            () -> assertNotNull(result, "Result should not be null"),
            () -> assertTrue(result.isEmpty(), "Result should be empty")
        );
    }

    /**
     * Tests error handling when database error occurs during notice retrieval
     */
    @Test
    void viewAllNotice_WhenSQLException_ShouldReturnEmptyList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery())
            .thenThrow(new SQLException("Database error"));

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertAll(
            () -> assertNotNull(result, "Result should not be null even after error"),
            () -> assertTrue(result.isEmpty(), "Result should be empty after error")
        );
    }

    /**
     * Tests successful notice addition
     */
    @Test
    void addNotice_WhenSuccessful_ShouldReturnSuccess() throws SQLException {
        // Arrange
        String noticeTitle = "Test Notice";
        String noticeDesc = "Test Info";
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = noticeDao.addNotice(noticeTitle, noticeDesc);

        // Assert
        assertAll(
            () -> assertEquals("Notice Added Successfully", result, 
                "Should return success message"),
            () -> verify(mockPreparedStatement).setString(1, noticeTitle),
            () -> verify(mockPreparedStatement).setString(2, noticeDesc)
        );
    }

    /**
     * Tests error handling during notice addition
     */
    @Test
    void addNotice_WhenSQLException_ShouldReturnError() throws SQLException {
        // Arrange
        String noticeTitle = "Test Notice";
        String noticeDesc = "Test Info";
        when(mockPreparedStatement.executeUpdate())
            .thenThrow(new SQLException("Database error"));

        // Act
        String result = noticeDao.addNotice(noticeTitle, noticeDesc);

        // Assert
        assertTrue(result.contains("Error:"), 
            "Should return error message when database operation fails");
    }

    /**
     * Tests successful notice removal
     */
    @Test
    void removeNotice_WhenSuccessful_ShouldReturnSuccess() throws SQLException {
        // Arrange
        int noticeId = 123;
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = noticeDao.removeNotice(noticeId);

        // Assert
        assertAll(
            () -> assertEquals("Notice No: " + noticeId + " has been Removed Successfully!", 
                result, "Should return success message"),
            () -> verify(mockPreparedStatement).setInt(1, noticeId)
        );
    }

    /**
     * Tests notice removal when notice doesn't exist
     */
    @Test
    void removeNotice_WhenNoticeNotFound_ShouldReturnNotFound() throws SQLException {
        // Arrange
        int noticeId = 123;
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        String result = noticeDao.removeNotice(noticeId);

        // Assert
        assertEquals("Notice Deletion Failed", result, 
            "Should return failure message when notice not found");
    }

    /**
     * Tests error handling during notice removal
     */
    @Test
    void removeNotice_WhenSQLException_ShouldReturnError() throws SQLException {
        // Arrange
        int noticeId = 123;
        when(mockPreparedStatement.executeUpdate())
            .thenThrow(new SQLException("Database error"));

        // Act
        String result = noticeDao.removeNotice(noticeId);

        // Assert
        assertTrue(result.contains("Error:"), 
            "Should return error message when database operation fails");
    }

    /**
     * Helper method to verify notice properties
     */
    private void assertNotice(NoticeBean notice, int expectedId, 
            String expectedTitle, String expectedInfo) {
        assertAll(
            () -> assertEquals(expectedId, notice.getNoticeId(), 
                "Notice ID should match"),
            () -> assertEquals(expectedTitle, notice.getNoticeTitle(), 
                "Notice title should match"),
            () -> assertEquals(expectedInfo, notice.getNoticeInfo(), 
                "Notice info should match")
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
