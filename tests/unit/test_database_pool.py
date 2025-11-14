"""
Unit tests for database connection pool
Best Practices: Comprehensive testing of connection pooling
"""

import pytest
import asyncio
from unittest.mock import AsyncMock, patch, MagicMock
from src.database import create_pool, close_pool, get_pool, check_pool_health, get_db_connection


@pytest.mark.asyncio
async def test_create_pool_success():
    """Test successful pool creation"""
    with patch('src.database.asyncpg.create_pool') as mock_create_pool:
        mock_pool = AsyncMock()
        mock_create_pool.return_value = mock_pool
        
        pool = await create_pool()
        
        assert pool == mock_pool
        mock_create_pool.assert_called_once()


@pytest.mark.asyncio
async def test_create_pool_retry_logic():
    """Test retry logic on failure"""
    with patch('src.database.asyncpg.create_pool') as mock_create_pool:
        mock_create_pool.side_effect = [
            Exception("Connection failed"),
            Exception("Connection failed"),
            AsyncMock(),  # Success on third attempt
        ]
        
        with patch('asyncio.sleep', new_callable=AsyncMock):
            pool = await create_pool(max_retries=3, retry_delay=0.1)
            
            assert pool is not None
            assert mock_create_pool.call_count == 3


@pytest.mark.asyncio
async def test_create_pool_exponential_backoff():
    """Test exponential backoff retry"""
    with patch('src.database.asyncpg.create_pool') as mock_create_pool:
        mock_create_pool.side_effect = Exception("Connection failed")
        
        sleep_calls = []
        async def mock_sleep(delay):
            sleep_calls.append(delay)
        
        with patch('asyncio.sleep', side_effect=mock_sleep):
            with pytest.raises(Exception):
                await create_pool(max_retries=3, retry_delay=1)
            
            # Check exponential backoff: 1, 2, 4 seconds
            assert len(sleep_calls) == 2
            assert sleep_calls[0] == 1  # 1 * 2^0
            assert sleep_calls[1] == 2  # 1 * 2^1


@pytest.mark.asyncio
async def test_get_pool_before_initialization():
    """Test get_pool raises error if pool not initialized"""
    # Reset pool
    import src.database
    src.database._pool = None
    
    with pytest.raises(RuntimeError, match="Database pool not initialized"):
        get_pool()


@pytest.mark.asyncio
async def test_check_pool_health_healthy():
    """Test pool health check when healthy"""
    mock_pool = AsyncMock()
    mock_pool.get_size.return_value = 10
    mock_pool.get_idle_size.return_value = 5
    
    mock_conn = AsyncMock()
    mock_conn.__aenter__ = AsyncMock(return_value=mock_conn)
    mock_conn.__aexit__ = AsyncMock(return_value=None)
    mock_pool.acquire.return_value = mock_conn
    
    with patch('src.database._pool', mock_pool):
        health = await check_pool_health()
        
        assert health["status"] == "healthy"
        assert health["pool_size"] == 10
        assert health["pool_idle_size"] == 5
        assert "response_time_ms" in health


@pytest.mark.asyncio
async def test_check_pool_health_unhealthy():
    """Test pool health check when unhealthy"""
    import src.database
    src.database._pool = None
    
    health = await check_pool_health()
    
    assert health["status"] == "unhealthy"
    assert "error" in health


@pytest.mark.asyncio
async def test_get_db_connection_context_manager():
    """Test database connection context manager"""
    mock_pool = AsyncMock()
    mock_conn = AsyncMock()
    mock_conn.__aenter__ = AsyncMock(return_value=mock_conn)
    mock_conn.__aexit__ = AsyncMock(return_value=None)
    mock_pool.acquire.return_value = mock_conn
    
    with patch('src.database.get_pool', return_value=mock_pool):
        async with get_db_connection() as conn:
            assert conn == mock_conn
        
        mock_conn.__aexit__.assert_called_once()


@pytest.mark.asyncio
async def test_close_pool_gracefully():
    """Test graceful pool closure"""
    mock_pool = AsyncMock()
    
    import src.database
    src.database._pool = mock_pool
    
    await close_pool()
    
    mock_pool.terminate.assert_called_once()
    assert src.database._pool is None

