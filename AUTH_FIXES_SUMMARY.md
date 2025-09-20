# Authentication Flow Fixes Summary

## Issues Identified and Fixed

### 1. API URL Mismatch
**Problem**: Frontend was calling `/api/v1/auth/*` but backend had `/api/auth/*`
**Solution**: 
- Updated `AuthController.java` to use `/api/v1/auth` mapping
- Updated `AuthV1Controller.java` to include missing `/profile` endpoint
- Updated frontend API client to use correct endpoints

### 2. Response Structure Mismatch
**Problem**: Backend returns `accessToken` but frontend expected `token`
**Solution**:
- Updated frontend `AuthResponse` interface to match backend
- Updated login page to use `response.accessToken`
- Made `tokenType` and `requires2FA` optional in frontend types

### 3. Multiple Auth Stores
**Problem**: Two conflicting auth stores (`stores/authStore.ts` and `lib/state/auth.ts`)
**Solution**:
- Removed duplicate `stores/authStore.ts`
- Updated all imports to use `lib/state/auth.ts`
- Ensured consistent token storage keys (`auth_token`, `refresh_token`)

### 4. Token Storage Inconsistency
**Problem**: Different services used different token keys
**Solution**:
- Updated `services/api.ts` to use `auth_token` instead of `token`
- Updated API client to use consistent token storage
- Added proper token cleanup on logout

### 5. Missing Profile Endpoint
**Problem**: Frontend called `/profile` but backend only had `/me`
**Solution**:
- Added `/profile` endpoint to `AuthV1Controller.java`
- Maintained backward compatibility with `/me` endpoint

### 6. User DTO Compatibility
**Problem**: Backend UserDto didn't match frontend User interface
**Solution**:
- Added computed fields (`name`, `isVerified`, `avatar`) to UserDto
- Updated frontend User interface to accept `string | number` for ID

### 7. Token Refresh Implementation
**Problem**: No automatic token refresh on 401 errors
**Solution**:
- Added token refresh interceptor to API client
- Automatic retry of failed requests with new token
- Proper fallback to login page on refresh failure

## Files Modified

### Backend (auth-service)
- `src/main/java/com/hopngo/auth/controller/AuthController.java`
- `src/main/java/com/hopngo/auth/controller/AuthV1Controller.java`
- `src/main/java/com/hopngo/auth/dto/UserDto.java`

### Frontend
- `src/lib/api/client.ts`
- `src/lib/api/auth.ts`
- `src/lib/api/types.ts`
- `src/services/api.ts`
- `src/app/[locale]/(app)/layout.tsx`
- `src/components/bd/BdNavbar.tsx`
- `src/components/referral/ReferralDashboard.tsx`
- `src/components/referral/InviteFriends.tsx`
- `src/app/[locale]/(app)/settings/page.tsx`
- `src/app/[locale]/(app)/trips/TripsClient.tsx`

### Removed Files
- `src/stores/authStore.ts` (duplicate)

## Testing

A test script `test-auth-flow.js` has been created to verify:
1. User registration
2. User profile retrieval
3. Token refresh
4. User login

## Next Steps

1. Start the auth service: `cd auth-service && mvn spring-boot:run`
2. Start the frontend: `cd frontend && npm run dev`
3. Run the test script: `node test-auth-flow.js`
4. Test the frontend login flow manually

## API Endpoints

All endpoints are now available under `/api/v1/auth/`:
- `POST /register` - Register new user
- `POST /login` - User login
- `POST /refresh` - Refresh access token
- `POST /logout` - User logout
- `GET /profile` - Get current user profile
- `POST /forgot-password` - Request password reset
- `POST /reset-password` - Reset password

The authentication flow should now work seamlessly between frontend and backend.
