import { Switch, Route } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider } from "@/contexts/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import NotFound from "@/pages/not-found";
import Dashboard from "@/pages/dashboard";
import CoursesCatalog from "@/pages/courses-catalog";
import CourseSelection from "@/pages/course-selection";
import AdminApproval from "@/pages/admin-approval";
import CourseManagementPage from "@/pages/course-management";
import CourseLeadingPage from "@/pages/course-leading";
import Certificates from "@/pages/certificates";
import Chat from "@/pages/chat";
import CalendarPage from "@/pages/calendar";
import AuthPage from "@/pages/auth";
import CourseDetail from "@/pages/course-detail";
import AnalyticsPage from "@/pages/analytics";
import NotificationsPage from "@/pages/notifications";
import CabinetProfilePage from "@/pages/cabinet-profile";
import CabinetProgressPage from "@/pages/cabinet-progress";
import CabinetHistoryPage from "@/pages/cabinet-history";
import CabinetSettingsPage from "@/pages/cabinet-settings";

function Router() {
  return (
    <Switch>
      <Route path="/auth" component={AuthPage} />
      <Route path="/">
        <ProtectedRoute>
          <Dashboard />
        </ProtectedRoute>
      </Route>
      <Route path="/catalog">
        <ProtectedRoute>
          <CoursesCatalog />
        </ProtectedRoute>
      </Route>
      <Route path="/selection">
        <ProtectedRoute>
          <CourseSelection />
        </ProtectedRoute>
      </Route>
      <Route path="/admin">
        <ProtectedRoute>
          <AdminApproval />
        </ProtectedRoute>
      </Route>
      <Route path="/admin/courses">
        <ProtectedRoute>
          <CourseManagementPage />
        </ProtectedRoute>
      </Route>
      <Route path="/teaching">
        <ProtectedRoute>
          <CourseLeadingPage />
        </ProtectedRoute>
      </Route>
      <Route path="/certificates">
        <ProtectedRoute>
          <Certificates />
        </ProtectedRoute>
      </Route>
      <Route path="/chat">
        <ProtectedRoute>
          <Chat />
        </ProtectedRoute>
      </Route>
      <Route path="/calendar">
        <ProtectedRoute>
          <CalendarPage />
        </ProtectedRoute>
      </Route>
      <Route path="/analytics">
        <ProtectedRoute>
          <AnalyticsPage />
        </ProtectedRoute>
      </Route>
      <Route path="/notifications">
        <ProtectedRoute>
          <NotificationsPage />
        </ProtectedRoute>
      </Route>
      <Route path="/course/:id">
        <ProtectedRoute>
          <CourseDetail />
        </ProtectedRoute>
      </Route>
      <Route path="/cabinet">
        <ProtectedRoute>
          <CabinetProfilePage />
        </ProtectedRoute>
      </Route>
      <Route path="/cabinet/profile">
        <ProtectedRoute>
          <CabinetProfilePage />
        </ProtectedRoute>
      </Route>
      <Route path="/cabinet/progress">
        <ProtectedRoute>
          <CabinetProgressPage />
        </ProtectedRoute>
      </Route>
      <Route path="/cabinet/history">
        <ProtectedRoute>
          <CabinetHistoryPage />
        </ProtectedRoute>
      </Route>
      <Route path="/cabinet/settings">
        <ProtectedRoute>
          <CabinetSettingsPage />
        </ProtectedRoute>
      </Route>
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TooltipProvider>
          <Toaster />
          <Router />
        </TooltipProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
