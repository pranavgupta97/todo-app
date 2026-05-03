/**
 * Custom Spring Security types — extensions of Spring's auth machinery
 * (e.g. {@link com.pranavgupta.todoapp.security.AppOidcUser AppOidcUser},
 * which carries our app's {@code User.id} on the OIDC principal so
 * controllers can read it without a per-request DB lookup).
 */
package com.pranavgupta.todoapp.security;
