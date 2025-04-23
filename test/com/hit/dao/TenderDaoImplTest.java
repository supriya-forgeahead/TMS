package com.hit.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.Date;
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

import com.hit.beans.TenderBean;
import com.hit.utility.DBUtil;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TenderDaoImplTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private TenderDaoImpl tenderDao;
    private MockedStatic<DBUtil> dbUtilMocked;

    @BeforeEach
    void setUp() throws SQLException {
        tenderDao = new TenderDaoImpl();
        dbUtilMocked = mockStatic(DBUtil.class);
        dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        if (dbUtilMocked != null) {
            dbUtilMocked.close();
        }
    }

    @Test
    void createTender_WhenSuccessful_ShouldReturnSuccessMessage() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        TenderBean tender = new TenderBean("T1", "Test Tender", "Construction", 
            1000, "Test Description", "2024-12-31", "Test Location");

        // Act
        String result = tenderDao.createTender(tender);

        // Assert
        assertEquals("New Tender Inserted<br> Your Tender id: T1", result);
    }

    @Test
    void createTender_WhenSQLException_ShouldReturnErrorMessage() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate())
            .thenThrow(new SQLException("Database error"));
        TenderBean tender = new TenderBean("T1", "Test Tender", "Construction", 
            1000, "Test Description", "2024-12-31", "Test Location");

        // Act
        String result = tenderDao.createTender(tender);

        // Assert
        assertEquals("Error : Database error", result);
    }

    @Test
    void removeTender_WhenSuccessful_ShouldReturnTrue() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Act
        boolean result = tenderDao.removeTender("T1");

        // Assert
        assertTrue(result);
    }

    @Test
    void removeTender_WhenSQLException_ShouldReturnFalse() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate())
            .thenThrow(new SQLException("Database error"));

        // Act
        boolean result = tenderDao.removeTender("T1");

        // Assert
        assertFalse(result);
    }

    @Test
    void updateTender_WhenSuccessful_ShouldReturnSuccessMessage() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        TenderBean tender = new TenderBean("T1", "Test Tender", "Construction", 
            1000, "Test Description", "2024-12-31", "Test Location");

        // Act
        String result = tenderDao.updateTender(tender);

        // Assert
        assertEquals("TENDER DETAILS UPDATED SUCCSESFULLY", result);
    }

    @Test
    void updateTender_WhenSQLException_ShouldReturnErrorMessage() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeUpdate())
            .thenThrow(new SQLException("Database error"));
        TenderBean tender = new TenderBean("T1", "Test Tender", "Construction", 
            1000, "Test Description", "2024-12-31", "Test Location");

        // Act
        String result = tenderDao.updateTender(tender);

        // Assert
        assertEquals("Error: Database error", result);
    }

    @Test
    void getAllTenders_WhenExists_ShouldReturnTenderList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        
        // Mock date value
        Date mockDate = new Date(System.currentTimeMillis());
        
        // Mock column values matching exact database column names
        when(mockResultSet.getString("tid")).thenReturn("T1");
        when(mockResultSet.getString("tname")).thenReturn("Test Tender 1");
        when(mockResultSet.getString("ttype")).thenReturn("Construction");
        when(mockResultSet.getInt("tprice")).thenReturn(1000);
        when(mockResultSet.getString("tdesc")).thenReturn("Description 1");
        when(mockResultSet.getDate(6)).thenReturn(mockDate);
        when(mockResultSet.getString("tloc")).thenReturn("Location 1");

        // Act
        List<TenderBean> result = tenderDao.getAllTenders();

        // Assert
        assertAll(
            "Verify tender list contents",
            () -> assertNotNull(result, "Result list should not be null"),
            () -> assertEquals(1, result.size(), "Should return 1 tender"),
            () -> assertNotNull(result.get(0), "First tender should not be null"),
            () -> assertAll("First tender properties",
                () -> assertEquals("T1", result.get(0).getId()),
                () -> assertEquals("Test Tender 1", result.get(0).getName()),
                () -> assertEquals("Construction", result.get(0).getType()),
                () -> assertEquals(1000, result.get(0).getPrice()),
                () -> assertEquals("Description 1", result.get(0).getDesc()),
                () -> assertNotNull(result.get(0).getDeadline(), "Deadline should not be null"),
                () -> assertEquals("Location 1", result.get(0).getLocation())
            )
        );
    }

    @Test
    void getAllTenders_WhenEmpty_ShouldReturnEmptyList() throws SQLException {
        // Arrange
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Act
        List<TenderBean> result = tenderDao.getAllTenders();

        // Assert
        assertAll(
            "Verify empty list",
            () -> assertNotNull(result, "Result list should not be null"),
            () -> assertTrue(result.isEmpty(), "Result list should be empty")
        );
    }
}
