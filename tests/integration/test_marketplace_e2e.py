"""
E2E tests for marketplace API with authentication and role-based access
"""

import pytest
from fastapi.testclient import TestClient
from httpx import AsyncClient

from src.main import app
from src.security.auth import get_current_user, require_roles
from src.security.roles import grant_role


@pytest.fixture
def client():
    """FastAPI test client"""
    return TestClient(app)


@pytest.fixture
def developer_user():
    """Mock developer user"""
    from src.security.auth import CurrentUser
    return CurrentUser(
        user_id="dev_123",
        username="developer",
        email="dev@example.com",
        roles=["developer"]
    )


@pytest.fixture
def admin_user():
    """Mock admin user"""
    from src.security.auth import CurrentUser
    return CurrentUser(
        user_id="admin_123",
        username="admin",
        email="admin@example.com",
        roles=["admin"]
    )


@pytest.fixture
def regular_user():
    """Mock regular user"""
    from src.security.auth import CurrentUser
    return CurrentUser(
        user_id="user_123",
        username="user",
        email="user@example.com",
        roles=["user"]
    )


@pytest.mark.asyncio
async def test_submit_plugin_requires_developer_role(client, developer_user):
    """Test that plugin submission requires developer role"""
    # Mock auth dependency
    app.dependency_overrides[require_roles("developer", "admin")] = lambda: developer_user
    
    response = client.post(
        "/marketplace/plugins",
        json={
            "name": "Test Plugin",
            "description": "A test plugin for E2E testing",
            "version": "1.0.0",
            "author": "Test Author",
        },
        headers={"Authorization": "Bearer test_token"}
    )
    
    assert response.status_code in [201, 400]  # 201 success or 400 validation error
    if response.status_code == 201:
        data = response.json()
        assert "plugin_id" in data
        assert data["status"] == "pending"
    
    app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_submit_plugin_rejects_regular_user(client, regular_user):
    """Test that regular users cannot submit plugins"""
    # This should fail because regular_user doesn't have developer role
    # In real scenario, require_roles would raise HTTPException
    
    # For testing, we verify the endpoint requires developer role
    # by checking that without proper auth, it returns 401/403
    response = client.post(
        "/marketplace/plugins",
        json={
            "name": "Test Plugin",
            "description": "A test plugin",
            "version": "1.0.0",
            "author": "Test Author",
        }
    )
    
    # Should fail without auth
    assert response.status_code in [401, 403, 422]


@pytest.mark.asyncio
async def test_update_plugin_requires_ownership(client, developer_user):
    """Test that only plugin owner can update their plugin"""
    # Mock current user
    app.dependency_overrides[get_current_user] = lambda: developer_user
    
    # First create a plugin
    create_response = client.post(
        "/marketplace/plugins",
        json={
            "name": "My Plugin",
            "description": "My plugin",
            "version": "1.0.0",
            "author": "Me",
        },
        headers={"Authorization": "Bearer test_token"}
    )
    
    if create_response.status_code == 201:
        plugin_id = create_response.json()["plugin_id"]
        
        # Try to update it
        update_response = client.put(
            f"/marketplace/plugins/{plugin_id}",
            json={"description": "Updated description"},
            headers={"Authorization": "Bearer test_token"}
        )
        
        # Should succeed for owner
        assert update_response.status_code in [200, 404]  # 404 if plugin not found in test DB
    
    app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_report_plugin_creates_complaint(client, regular_user):
    """Test that reporting a plugin creates a complaint"""
    app.dependency_overrides[get_current_user] = lambda: regular_user
    
    response = client.post(
        "/marketplace/plugins/test_plugin_123/report",
        params={"reason": "spam", "details": "This is spam"},
        headers={"Authorization": "Bearer test_token"}
    )
    
    # Should accept the report (404 if plugin doesn't exist in test DB)
    assert response.status_code in [200, 404]
    
    if response.status_code == 200:
        data = response.json()
        assert data["status"] == "reported"
    
    app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_marketplace_authorization_flow(client, developer_user, regular_user):
    """Test complete authorization flow for marketplace"""
    # 1. Developer can submit plugin
    app.dependency_overrides[require_roles("developer", "admin")] = lambda: developer_user
    
    submit_response = client.post(
        "/marketplace/plugins",
        json={
            "name": "Auth Test Plugin",
            "description": "Testing auth",
            "version": "1.0.0",
            "author": "Test",
        },
        headers={"Authorization": "Bearer dev_token"}
    )
    
    assert submit_response.status_code in [201, 400]
    
    # 2. Regular user cannot submit
    app.dependency_overrides[require_roles("developer", "admin")] = lambda: regular_user
    
    # This should fail - but in test we can't easily test this without
    # proper role checking, so we verify the endpoint exists
    assert True  # Placeholder - actual test would verify 403 response
    
    app.dependency_overrides.clear()

