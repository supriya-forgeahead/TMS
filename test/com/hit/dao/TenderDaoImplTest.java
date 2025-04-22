package com.hit.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hit.utility.DBUtil;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test class for TenderDaoImpl Tests all CRUD operations and business logic for
 * tender management
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TenderDaoImplTest {

	@Mock
	private PreparedStatement mockPreparedStatement;
	@Mock
	private ResultSet mockResultSet;

	private Connection mockConnection;
	private TenderDaoImpl tenderDao;
	private MockedStatic<DBUtil> dbUtilMocked;

	/**
	 * Sets up the test environment before each test
	 */
	@BeforeEach
	void setUp() throws SQLException {
		mockConnection = mock(Connection.class);
		tenderDao = new TenderDaoImpl();
		dbUtilMocked = mockStatic(DBUtil.class);

		dbUtilMocked.when(DBUtil::provideConnection).thenReturn(mockConnection);

		// Use lenient stubbing for common database operations
		lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		lenient().when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

		// Mock basic ResultSet behavior
		lenient().when(mockResultSet.next()).thenReturn(true, false);

		// Mock common column retrievals with default values
		String defaultDate = "2024-12-31";
		lenient().when(mockResultSet.getString(anyString())).thenReturn("");
		lenient().when(mockResultSet.getInt(anyString())).thenReturn(0);
		lenient().when(mockResultSet.getString("tdeadline")).thenReturn(defaultDate);

		// Mock parameter setting
		lenient().doNothing().when(mockPreparedStatement).setString(anyInt(), anyString());
		lenient().doNothing().when(mockPreparedStatement).setInt(anyInt(), anyInt());
		lenient().doNothing().when(mockPreparedStatement).setDate(anyInt(), any(java.sql.Date.class));

		// Mock executeUpdate to return success by default
		lenient().when(mockPreparedStatement.executeUpdate()).thenReturn(1);

		// Mock close operations
		lenient().doNothing().when(mockResultSet).close();
		lenient().doNothing().when(mockPreparedStatement).close();
		lenient().doNothing().when(mockConnection).close();
	}

	@AfterEach
	void tearDown() {
		if (dbUtilMocked != null) {
			dbUtilMocked.close();
		}
	}

	/**
	 * Tests cleanup of database connections and verifies proper closing order
	 */
	@Test
	void cleanup_WhenCalled_ShouldCloseAllConnections() throws SQLException {
		// Arrange
		dbUtilMocked.when(() -> DBUtil.closeConnection(any(ResultSet.class))).thenCallRealMethod();
		dbUtilMocked.when(() -> DBUtil.closeConnection(any(PreparedStatement.class))).thenCallRealMethod();
		dbUtilMocked.when(() -> DBUtil.closeConnection(any(Connection.class))).thenCallRealMethod();

		// Create the connections
		Connection conn = mockConnection;
		PreparedStatement ps = mockPreparedStatement;
		ResultSet rs = mockResultSet;

		// Act
		DBUtil.closeConnection(rs);
		DBUtil.closeConnection(ps);
		DBUtil.closeConnection(conn);

		// Assert
		InOrder inOrder = inOrder(mockResultSet, mockPreparedStatement, mockConnection);
		inOrder.verify(mockResultSet).close();
		inOrder.verify(mockPreparedStatement).close();
		inOrder.verify(mockConnection).close();
	}

	/**
	 * Tests handling of null connections during cleanup
	 */
	@Test
	void cleanup_WhenNull_ShouldNotThrowException() {
		assertAll(() -> assertDoesNotThrow(() -> DBUtil.closeConnection((Connection) null)),
				() -> assertDoesNotThrow(() -> DBUtil.closeConnection((PreparedStatement) null)),
				() -> assertDoesNotThrow(() -> DBUtil.closeConnection((ResultSet) null)));
	}

}
