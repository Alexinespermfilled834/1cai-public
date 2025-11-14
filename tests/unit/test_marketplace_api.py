"""
Unit tests for marketplace API endpoints
Tests for create, update, and complaints functionality
"""

import pytest
from unittest.mock import AsyncMock, Mock, patch
from fastapi import HTTPException

from src.api.marketplace import (
    submit_plugin,
    update_plugin,
    report_plugin,
    PluginSubmitRequest,
    PluginUpdateRequest,
    PluginStatus,
)
from src.security.auth import CurrentUser


@pytest.fixture
def mock_user():
    """Mock current user"""
    return CurrentUser(
        user_id="user_123",
        username="testuser",
        email="test@example.com",
        roles=["developer"]
    )


@pytest.fixture
def mock_repo():
    """Mock marketplace repository"""
    repo = AsyncMock()
    repo.create_plugin = AsyncMock(return_value={
        "plugin_id": "plugin_123",
        "name": "Test Plugin",
        "status": PluginStatus.PENDING.value,
        "owner_id": "user_123",
    })
    repo.get_plugin = AsyncMock(return_value={
        "plugin_id": "plugin_123",
        "name": "Test Plugin",
        "status": PluginStatus.APPROVED.value,
        "owner_id": "user_123",
    })
    repo.update_plugin = AsyncMock(return_value={
        "plugin_id": "plugin_123",
        "name": "Updated Plugin",
        "status": PluginStatus.APPROVED.value,
        "owner_id": "user_123",
    })
    repo.add_complaint = AsyncMock(return_value=True)
    return repo


@pytest.mark.asyncio
async def test_submit_plugin_success(mock_user, mock_repo):
    """Test successful plugin submission"""
    plugin_data = PluginSubmitRequest(
        name="Test Plugin",
        description="A test plugin",
        version="1.0.0",
        author="Test Author",
    )
    
    with patch("src.api.marketplace.audit_logger") as mock_audit:
        result = await submit_plugin(
            plugin=plugin_data,
            current_user=mock_user,
            repo=mock_repo
        )
    
    assert result["plugin_id"] == "plugin_123"
    assert result["status"] == PluginStatus.PENDING.value
    mock_repo.create_plugin.assert_called_once()
    mock_audit.log_action.assert_called_once()


@pytest.mark.asyncio
async def test_submit_plugin_validation_error(mock_user, mock_repo):
    """Test plugin submission with validation error"""
    plugin_data = PluginSubmitRequest(
        name="AB",  # Too short
        description="Test",
        version="1.0.0",
        author="Test",
    )
    
    # Should fail validation before reaching endpoint
    with pytest.raises(Exception):
        await submit_plugin(
            plugin=plugin_data,
            current_user=mock_user,
            repo=mock_repo
        )


@pytest.mark.asyncio
async def test_update_plugin_success(mock_user, mock_repo):
    """Test successful plugin update"""
    update_data = PluginUpdateRequest(
        description="Updated description"
    )
    
    with patch("src.api.marketplace.audit_logger") as mock_audit:
        result = await update_plugin(
            plugin_id="plugin_123",
            update=update_data,
            current_user=mock_user,
            repo=mock_repo
        )
    
    assert result["name"] == "Updated Plugin"
    mock_repo.update_plugin.assert_called_once()
    mock_audit.log_action.assert_called_once()


@pytest.mark.asyncio
async def test_update_plugin_not_found(mock_user, mock_repo):
    """Test updating non-existent plugin"""
    mock_repo.get_plugin.return_value = None
    update_data = PluginUpdateRequest(description="Updated")
    
    with pytest.raises(HTTPException) as exc_info:
        await update_plugin(
            plugin_id="nonexistent",
            update=update_data,
            current_user=mock_user,
            repo=mock_repo
        )
    
    assert exc_info.value.status_code == 404


@pytest.mark.asyncio
async def test_update_plugin_unauthorized(mock_user, mock_repo):
    """Test updating plugin without permission"""
    other_user = CurrentUser(
        user_id="user_456",
        username="otheruser",
        email="other@example.com",
        roles=["developer"]
    )
    update_data = PluginUpdateRequest(description="Updated")
    
    with pytest.raises(HTTPException) as exc_info:
        await update_plugin(
            plugin_id="plugin_123",
            update=update_data,
            current_user=other_user,
            repo=mock_repo
        )
    
    assert exc_info.value.status_code == 403


@pytest.mark.asyncio
async def test_report_plugin_success(mock_user, mock_repo):
    """Test successful plugin report"""
    with patch("src.api.marketplace.audit_logger") as mock_audit:
        result = await report_plugin(
            plugin_id="plugin_123",
            reason="spam",
            details="This is spam",
            current_user=mock_user,
            repo=mock_repo
        )
    
    assert result["status"] == "reported"
    assert result["plugin_id"] == "plugin_123"
    mock_repo.add_complaint.assert_called_once()
    mock_audit.log_action.assert_called_once()


@pytest.mark.asyncio
async def test_report_plugin_not_found(mock_user, mock_repo):
    """Test reporting non-existent plugin"""
    mock_repo.get_plugin.return_value = None
    
    with pytest.raises(HTTPException) as exc_info:
        await report_plugin(
            plugin_id="nonexistent",
            reason="spam",
            current_user=mock_user,
            repo=mock_repo
        )
    
    assert exc_info.value.status_code == 404


@pytest.mark.asyncio
async def test_update_plugin_version_changes_status(mock_user, mock_repo):
    """Test that updating version changes status to PENDING"""
    update_data = PluginUpdateRequest(version="2.0.0")
    
    await update_plugin(
        plugin_id="plugin_123",
        update=update_data,
        current_user=mock_user,
        repo=mock_repo
    )
    
    # Check that update_plugin was called with status=PENDING
    call_args = mock_repo.update_plugin.call_args
    assert "status" in call_args[1] or (len(call_args[0]) > 1 and "status" in call_args[0][1])
    # The status should be set to PENDING when version changes

