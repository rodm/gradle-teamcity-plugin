<%@ page import="jetbrains.buildServer.util.StringUtil" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ include file="/include.jsp" %>

<c:set var="overrideBundleUrl" value="${intprop:getProperty('teamcity.plugins.SakuraUI-Plugin.bundleUrl', '')}" />

<c:choose>
  <c:when test="${!StringUtil.isEmpty(overrideBundleUrl)}">
    <script src="<c:out value="${overrideBundleUrl}" />"></script>
  </c:when>
  <c:otherwise>
    <bs:linkScript>${teamcityPluginResourcesPath}bundle.js</bs:linkScript>
  </c:otherwise>
</c:choose>
