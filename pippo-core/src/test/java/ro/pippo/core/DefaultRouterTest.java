/*
 * Copyright (C) 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.pippo.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ro.pippo.core.route.DefaultRouter;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.RouteGroup;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.route.RouteMatch;
import ro.pippo.core.route.WebjarsResourceHandler;
import ro.pippo.core.util.PathRegexBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author Decebal Suiu
 */
public class DefaultRouterTest {

    private static final RouteHandler emptyRouteHandler = new EmptyRouteHandler();

    private DefaultRouter router;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        router = new DefaultRouter();
    }

    @After
    public void after() {
        router = null;
    }

    @Test
    public void testNullUriPatternRoute() throws Exception {
        Route route = Route.GET(null, emptyRouteHandler);
        thrown.expect(Exception.class);
        thrown.expectMessage("The uri pattern cannot be null or empty");
        router.addRoute(route);
    }

    @Test
    public void testEmptyUriPatternRoute() throws Exception {
        Route route = Route.GET("", emptyRouteHandler);
        thrown.expect(Exception.class);
        thrown.expectMessage("The uri pattern cannot be null or empty");
        router.addRoute(route);
    }

    @Test
    public void testUnspecifiedMethodRequestRoute() throws Exception {
        Route route = new Route("", "/.*", emptyRouteHandler);
        thrown.expect(Exception.class);
        thrown.expectMessage("Unspecified request method");
        router.addRoute(route);
    }

    @Test
    public void testAddRoute() throws Exception {
        Route route = Route.GET("/.*", emptyRouteHandler);
        router.addRoute(route);

        assertEquals(1, router.getRoutes().size());
        assertEquals(1, router.getRoutes(HttpConstants.Method.GET).size());
    }

    @Test
    public void testRemoveRoute() throws Exception {
        Route route = Route.GET("/.*", emptyRouteHandler);
        router.addRoute(route);

        assertEquals(1, router.getRoutes().size());
        assertEquals(1, router.getRoutes(HttpConstants.Method.GET).size());

        router.removeRoute(route);
        assertEquals(0, router.getRoutes().size());
        assertEquals(0, router.getRoutes(HttpConstants.Method.GET).size());
    }

    @Test
    public void testFindRoutes() throws Exception {
        Route route = Route.GET("/contact", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact");
        assertNotNull(routeMatches);
        assertEquals(1, routeMatches.size());

        routeMatches = router.findRoutes(HttpConstants.Method.POST, "/contact");
        assertNotNull(routeMatches);
        assertEquals(0, routeMatches.size());

        routeMatches = router.findRoutes(HttpConstants.Method.GET, "/");
        assertNotNull(routeMatches);
        assertEquals(0, routeMatches.size());
    }

    @Test
    public void testPathParamsRoute() throws Exception {
        Route route = Route.GET("/contact/{id}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/3");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(1, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals(String.valueOf(3), pathParameters.get("id"));
    }

    @Test
    public void testWildcardRoute() throws Exception {
        Route route = Route.GET("/.*", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/3");
        assertEquals(1, routeMatches.size());
    }

    @Test
    public void testPatchRoute() throws Exception {
        Route route = Route.PATCH("/contact/{id}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.PATCH, "/contact/3");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(1, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals(String.valueOf(3), pathParameters.get("id"));
    }

    @Test
    public void testIntIdRoute() throws Exception {
        Route route = Route.PATCH("/contact/{id: [0-9]+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.PATCH, "/contact/3");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(1, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals(String.valueOf(3), pathParameters.get("id"));

        routeMatches = router.findRoutes(HttpConstants.Method.PATCH, "/contact/a");
        assertEquals(0, routeMatches.size());
    }

    @Test
    public void testIntIdRoute2() throws Exception {
        Route route = Route.GET("/contact/{id: [0-9]+}/something/{else: [A-z]*}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/3/something/borrowed");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals(String.valueOf(3), pathParameters.get("id"));
        assertEquals("borrowed", pathParameters.get("else"));
    }

    @Test
    public void testPosixAlpha() throws Exception {
        Route route = Route.GET("/user/{login: :alpha:+}/todo/{id: :digit:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/user/jämяs/todo/57");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("login"));
        assertEquals("jämяs", pathParameters.get("login"));
        assertTrue(pathParameters.containsKey("id"));
        assertEquals("57", pathParameters.get("id"));
    }

    @Test
    public void testPosixAlpha2() throws Exception {
        Route route = Route.GET("/user/{login: [:digit::alpha:-_\\+\\.]+}/todo/{id: :digit:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/user/j.ä_я3-s/todo/57");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("login"));
        assertEquals("j.ä_я3-s", pathParameters.get("login"));
        assertTrue(pathParameters.containsKey("id"));
        assertEquals("57", pathParameters.get("id"));
    }

    @Test
    public void testPosixAlnum() throws Exception {
        Route route = Route.GET("/user/{login: :alnum:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/user/james5");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(1, pathParameters.size());
        assertTrue(pathParameters.containsKey("login"));
        assertEquals("james5", pathParameters.get("login"));
    }

    @Test
    public void testPosixDigit() throws Exception {
        Route route = Route.GET("/contact/{id: :digit:+}/{field: :alpha:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/57/telephone");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals("57", pathParameters.get("id"));
        assertTrue(pathParameters.containsKey("field"));
        assertEquals("telephone", pathParameters.get("field"));
    }

    @Test
    public void testPosixHexDigit() throws Exception {
        Route route = Route.GET("/contact/{id: :xdigit:+}/{field: :digit:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/5ace076/97");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals("5ace076", pathParameters.get("id"));
        assertTrue(pathParameters.containsKey("field"));
        assertEquals("97", pathParameters.get("field"));
    }

    @Test
    public void testPosixASCII() throws Exception {
        Route route = Route.GET("/contact/{id: :ascii:+}/{field: :digit:+}", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/contact/5ace076/97");
        assertEquals(1, routeMatches.size());

        Map<String, String> pathParameters = routeMatches.get(0).getPathParameters();
        assertNotNull(pathParameters);
        assertEquals(2, pathParameters.size());
        assertTrue(pathParameters.containsKey("id"));
        assertEquals("5ace076", pathParameters.get("id"));
        assertTrue(pathParameters.containsKey("field"));
        assertEquals("97", pathParameters.get("field"));
    }

    @Test
    public void testWebjarsRoute() throws Exception {
        WebjarsResourceHandler webjars = new WebjarsResourceHandler();
        Route route = Route.GET(webjars.getUriPattern(), emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, "/webjars/bootstrap/3.0.2/css/bootstrap.min.css"
        );
        assertEquals(1, routeMatches.size());
    }

    @Test
    public void testParameters() throws Exception {
        // /////////////////////////////////////////////////////////////////////
        // One parameter:
        // /////////////////////////////////////////////////////////////////////
        router.addRoute(Route.GET("/{name}/dashboard", emptyRouteHandler));

        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "/dashboard").size());

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/John/dashboard");
        assertEquals(1, matches.size());
        assertEquals("John", matches.get(0).getPathParameters().get("name"));

        // /////////////////////////////////////////////////////////////////////
        // More parameters
        // /////////////////////////////////////////////////////////////////////
        router.addRoute(Route.GET("/{name}/{id}/dashboard", emptyRouteHandler));

        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "/dashboard").size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/John/20/dashboard");
        assertEquals(1, matches.size());
        assertEquals("John", matches.get(0).getPathParameters().get("name"));
        assertEquals("20", matches.get(0).getPathParameters().get("id"));
    }

    @Test
    public void testParametersAndRegex() throws Exception {
        router.addRoute(Route.GET("/John/{id}/.*", emptyRouteHandler));

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/John/20/dashboard");
        assertEquals(1, matches.size());
        RouteMatch match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("20", match.getPathParameters().get("id"));

        matches = router.findRoutes(HttpConstants.Method.GET, "/John/20/admin");
        assertEquals(1, matches.size());
        match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("20", match.getPathParameters().get("id"));

        matches = router.findRoutes(HttpConstants.Method.GET, "/John/20/mock");
        assertEquals(1, matches.size());
        match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("20", match.getPathParameters().get("id"));
    }

    @Test
    public void testParametersAndRegexInsideVariableParts() throws Exception {
        router.addRoute(Route.GET("/public/{path: .*}", emptyRouteHandler));

        String pathUnderTest = "/public/css/app.css";
        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, pathUnderTest);
        assertEquals(1, matches.size());
        RouteMatch match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("css/app.css", match.getPathParameters().get("path"));

        pathUnderTest = "/public/js/main.js";
        matches = router.findRoutes(HttpConstants.Method.GET, pathUnderTest);
        assertEquals(1, matches.size());
        match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("js/main.js", match.getPathParameters().get("path"));

        pathUnderTest = "/public/robots.txt";
        matches = router.findRoutes(HttpConstants.Method.GET, pathUnderTest);
        assertEquals(1, matches.size());
        match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("robots.txt", match.getPathParameters().get("path"));

        // multiple parameter parsing with regex expressions
        router.addRoute(Route.GET("/{name: .+}/photos/{id: [0-9]+}", emptyRouteHandler));

        pathUnderTest = "/John/photos/2201";
        matches = router.findRoutes(HttpConstants.Method.GET, pathUnderTest);
        assertEquals(1, matches.size());
        match = matches.get(0);
        assertEquals(2, match.getPathParameters().size());
        assertEquals("John", match.getPathParameters().get("name"));
        assertEquals("2201", match.getPathParameters().get("id"));

        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "John/photos/first").size());
    }

    @Test
    public void testParametersDontCrossSlashes() throws Exception {
        router.addRoute(Route.GET("/blah/{id}/{id2}/{id3}/morestuff/at/the/end", emptyRouteHandler));

        // this must match
        assertEquals(1, router.findRoutes(HttpConstants.Method.GET, "/blah/id/id2/id3/morestuff/at/the/end").size());

        // this should not match as the last "end" is missing
        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "/blah/id/id2/id3/morestuff/at/the").size());
    }

    @Test
    public void testPointsInRegexDontCrashRegexInTheMiddleOfTheRoute() throws Exception {
        router.addRoute(Route.GET("/blah/{id}/myname", emptyRouteHandler));

        // the "." in the route should not make any trouble:
        String routeFromServer = "/blah/my.id/myname";

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, routeFromServer);
        assertEquals(1, matches.size());
        RouteMatch match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("my.id", match.getPathParameters().get("id"));

        // and another slightly different route
        routeFromServer = "/blah/my.id/myname/should_not_match";
        matches = router.findRoutes(HttpConstants.Method.GET, routeFromServer);
        assertEquals(0, matches.size());
    }

    @Test
    public void testPointsInRegexDontCrashRegexAtEnd() throws Exception {
        router.addRoute(Route.GET("/blah/{id}", emptyRouteHandler));

        // the "." in the route should not make any trouble:
        // even if it's the last part of the route
        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/blah/my.id");
        assertEquals(1, matches.size());
        RouteMatch match = matches.get(0);
        assertEquals(1, match.getPathParameters().size());
        assertEquals("my.id", match.getPathParameters().get("id"));
    }

    @Test
    public void testRegexInRouteWorksWithEscapes() throws Exception {
        // Test escaped constructs in regex
        // regex with escaped construct in a route
        router.addRoute(Route.GET("/customers/\\d+", emptyRouteHandler));

        assertEquals(1, router.findRoutes(HttpConstants.Method.GET, "/customers/1234").size());
        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "/customers/12ab").size());

        // regex with escaped construct in a route with variable parts
        router = new DefaultRouter();
        router.addRoute(Route.GET("/customers/{id: \\d+}", emptyRouteHandler));

        assertEquals(1, router.findRoutes(HttpConstants.Method.GET, "/customers/1234").size());
        assertEquals(0, router.findRoutes(HttpConstants.Method.GET, "/customers/12x").size());

        RouteMatch routeMatch = router.findRoutes(HttpConstants.Method.GET, "/customers/1234").get(0);
        Map<String, String> map = routeMatch.getPathParameters();
        assertEquals(1, map.size());
        assertEquals("1234", map.get("id"));
    }

    @Test
    public void testRegexInRouteWorksWithoutSlashAtTheEnd() throws Exception {
        Route route = Route.GET("/blah/{id}/.*", emptyRouteHandler);
        router.addRoute(route);

        // the "." in the real route should work without any problems:
        String routeFromServer = "/blah/my.id/and/some/more/stuff";

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, routeFromServer);
        assertEquals(1, routeMatches.size());
        RouteMatch routeMatch = routeMatches.get(0);
        assertEquals(route, routeMatch.getRoute());

        assertEquals(1, routeMatch.getPathParameters().size());
        assertEquals("my.id", routeMatch.getPathParameters().get("id"));

        // another slightly different route.
        routeFromServer = "/blah/my.id/";

        routeMatches = router.findRoutes(HttpConstants.Method.GET, routeFromServer);
        assertEquals(1, routeMatches.size());
        routeMatch = routeMatches.get(0);
        assertEquals(route, routeMatch.getRoute());

        assertEquals(1, routeMatch.getPathParameters().size());
        assertEquals("my.id", routeMatch.getPathParameters().get("id"));

        routeMatches = router.findRoutes(HttpConstants.Method.GET, "/blah/my.id");
        assertEquals(0, routeMatches.size());
    }

    @Test
    public void testRouteWithUrlEncodedSlashGetsChoppedCorrectly() throws Exception {
        Route route = Route.GET("/blah/{id}/.*", emptyRouteHandler);
        router.addRoute(route);

        // Just a simple test to make sure everything works on a not encoded
        // uri: decoded this would be /blah/my/id/and/some/more/stuff
        String routeFromServer = "/blah/my%2fid/and/some/more/stuff";

        List<RouteMatch> routeMatches = router.findRoutes(HttpConstants.Method.GET, routeFromServer);
        assertEquals(1, routeMatches.size());
        RouteMatch routeMatch = routeMatches.get(0);
        assertEquals(route, routeMatch.getRoute());

        assertEquals(1, routeMatch.getPathParameters().size());
        assertEquals("my%2fid", routeMatch.getPathParameters().get("id"));
    }

    @Test
    public void testUriForWithRegex() throws Exception {
        Route route = Route.GET("/user/{email}/{id: .*}", emptyRouteHandler);
        router.addRoute(route);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", "test@test.com");
        parameters.put("id", 5);
        String path = router.uriFor(route.getUriPattern(), parameters);

        assertThat(path, equalTo("/user/test@test.com/5"));
    }

    @Test
    public void testUriForWithMultipleRegex() throws Exception {
        Route route = Route.GET("/user/{email: .*}/test/{id: .*}", emptyRouteHandler);
        router.addRoute(route);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", "test@test.com");
        parameters.put("id", 5);
        String path = router.uriFor(route.getUriPattern(), parameters);

        assertThat(path, equalTo("/user/test@test.com/test/5"));
    }

    @Test
    public void testUriForWithSplat() throws Exception {
        Route route = Route.GET("/repository/{repo: .*}/ticket/{id: .*}", emptyRouteHandler);
        router.addRoute(route);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("repo", "test/myrepo");
        parameters.put("id", 5);
        String path = router.uriFor(route.getUriPattern(), parameters);

        assertThat(path, equalTo("/repository/test/myrepo/ticket/5"));

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/repository/test/myrepo/ticket/5");

        assertFalse(matches.isEmpty());
        assertEquals("test/myrepo", matches.get(0).getPathParameters().get("repo"));
        assertEquals("5", matches.get(0).getPathParameters().get("id"));
    }

    @Test
    public void testUriForWithRegexAndQueryParameters() throws Exception {
        Route route = Route.GET("/user/{email}/{id: .*}", emptyRouteHandler);
        router.addRoute(route);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", "test@test.com");
        parameters.put("id", 5);
        parameters.put("query", "recent_changes");
        String path = router.uriFor(route.getUriPattern(), parameters);

        assertThat(path, equalTo("/user/test@test.com/5?query=recent_changes"));
    }

    @Test
    public void testUriForWithEncodedParameters() throws Exception {
        Route route = Route.GET("/user/{email}", emptyRouteHandler);
        router.addRoute(route);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", "test@test.com");
        parameters.put("name", "Decebal Suiu");
        String path = router.uriFor(route.getUriPattern(), parameters);

        assertThat(path, equalTo("/user/test@test.com?name=Decebal+Suiu"));
    }

    @Test
    public void testExclusionFilter() throws Exception {
        Route route = Route.ALL("^(?!/(webjars|public)/).*", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/test/route");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/webjars/route");
        assertEquals(0, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/public/route");
        assertEquals(0, matches.size());
    }

    @Test
    public void testOptionalSuffixGroup() throws Exception {
        Route route = Route.ALL("/api/contact/{id: [0-9]+}(\\.(json|xml|yaml))?", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.json");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.xml");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.yaml");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.unknown");
        assertEquals(0, matches.size());
    }

    @Test
    public void testRequiredSuffixGroup() throws Exception {
        Route route = Route.ALL("/api/contact/{id: [0-9]+}(\\.(json|xml|yaml))", emptyRouteHandler);
        router.addRoute(route);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5");
        assertEquals(0, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.json");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.xml");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.yaml");
        assertEquals(1, matches.size());

        matches = router.findRoutes(HttpConstants.Method.GET, "/api/contact/5.unknown");
        assertEquals(0, matches.size());
    }

    @Test
    public void testAddGroup() {
        RouteGroup group = new RouteGroup("/users");
        group.GET("{id}", emptyRouteHandler);

        router.addRouteGroup(group);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/users/1");
        assertEquals(1, matches.size());
    }

    @Test
    public void testAddRouteWithGroup() {
        RouteGroup group = new RouteGroup("/users");
        Route route = group.GET("{id}", emptyRouteHandler);

        router.addRoute(route);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/users/1");
        assertEquals(1, matches.size());
    }

    @Test
    public void testGroupAddRoute() {
        RouteGroup group = new RouteGroup("/users");
        Route route = Route.GET("{id}", emptyRouteHandler);
        group.addRoute(route);

        router.addRouteGroup(group);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/users/1");
        assertEquals(1, matches.size());
    }

    @Test
    public void testNestGroup() {
        RouteGroup group = new RouteGroup("/users");
        RouteGroup child = new RouteGroup(group, "{id}");
        Route route = Route.POST("like", emptyRouteHandler);
        child.addRoute(route);

        router.addRouteGroup(group);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.POST, "/users/1/like");
        assertEquals(1, matches.size());
    }

    @Test
    public void testCustomGroup() {
        UserGroup userGroup = new UserGroup();
        router.addRouteGroup(userGroup);

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/users");
        assertEquals(1, matches.size());
    }

    @Test
    public void testEmptyStringAsPathParameter() {
        router.addRoute(Route.GET("/", emptyRouteHandler));
        router.addRoute(Route.GET("/{id}", emptyRouteHandler));

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/");
        assertEquals(1, matches.size());
    }

    private class UserGroup extends RouteGroup {

        public UserGroup() {
            super("/users");

            GET(emptyRouteHandler);
        }

    }

    @Test
    public void testUnderlineInPathParameter() throws Exception {
        router.addRoute(Route.GET("/{user_id}", emptyRouteHandler));

        List<RouteMatch> matches = router.findRoutes(HttpConstants.Method.GET, "/123");
        assertEquals(1, matches.size());
    }

}
