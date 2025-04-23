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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hit.beans.NoticeBean;
import com.hit.utility.DBUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NoticeDaoImplTest {

    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private Connection mockConnection;

    private NoticeDaoImpl noticeDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    @BeforeEach
    void setUp() {
        noticeDao = new NoticeDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() {
        if (dbUtilMocked != null) {
            dbUtilMocked.close();
        }
    }

    @Test
    void viewAllNotice_WhenNoticesExist_ShouldReturnNoticeList() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(123, 124);
        when(mockResultSet.getString("title")).thenReturn("Test Notice 1", "Test Notice 2");
        when(mockResultSet.getString("info")).thenReturn("Info 1", "Info 2");

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(2, result.size()),
            () -> assertNotice(result.get(0), 123, "Test Notice 1", "Info 1"),
            () -> assertNotice(result.get(1), 124, "Test Notice 2", "Info 2")
        );
    }

    @Test
    void viewAllNotice_WhenNoNotices_ShouldReturnEmptyList() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void viewAllNotice_WhenSQLException_ShouldReturnEmptyList() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

        // Act
        List<NoticeBean> result = noticeDao.viewAllNotice();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void addNotice_WhenSuccessful_ShouldReturnSuccess() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = noticeDao.addNotice("Test Notice", "Test Info");

        // Assert
        assertEquals("Notice Added Successfully", result);
        verify(mockPreparedStatement).setString(1, "Test Notice");
        verify(mockPreparedStatement).setString(2, "Test Info");
    }

    @Test
    void addNotice_WhenSQLException_ShouldReturnError() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        String result = noticeDao.addNotice("Test Notice", "Test Info");

        // Assert
        assertEquals("Error: Database error", result);
    }

    @Test
    void removeNotice_WhenSuccessful_ShouldReturnSuccess() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        String result = noticeDao.removeNotice(123);

        // Assert
        assertEquals("Notice No: 123 has been Removed Successfully!", result);
        verify(mockPreparedStatement).setInt(1, 123);
    }

    @Test
    void removeNotice_WhenNoticeNotFound_ShouldReturnNotFound() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        String result = noticeDao.removeNotice(123);

        // Assert
        assertEquals("Notice Deletion Failed", result);
    }

    @Test
    void removeNotice_WhenSQLException_ShouldReturnError() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

        // Act
        String result = noticeDao.removeNotice(123);

        // Assert
        assertEquals("Error: Database error", result);
    }

    private void assertNotice(NoticeBean notice, int expectedId, String expectedTitle, String expectedInfo) {
        assertAll(
            () -> assertEquals(expectedId, notice.getNoticeId()),
            () -> assertEquals(expectedTitle, notice.getNoticeTitle()),
            () -> assertEquals(expectedInfo, notice.getNoticeInfo())
        );
    }
}
