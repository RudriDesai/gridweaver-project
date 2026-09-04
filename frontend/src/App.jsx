import { useEffect, useState } from "react";
import LandingPage from "./pages/LandingPage";
import Dashboard from "./pages/Dashboard";

function getViewFromHash() {
  return window.location.hash === "#dashboard" ? "dashboard" : "landing";
}

function App() {
  const [view, setView] = useState(getViewFromHash());

  useEffect(() => {
    const onHashChange = () => setView(getViewFromHash());
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  const openDashboard = () => {
    window.location.hash = "#dashboard";
    setView("dashboard");
  };

  const goHome = () => {
    window.location.hash = "";
    setView("landing");
  };

  if (view === "dashboard") {
    return <Dashboard onNavigateHome={goHome} />;
  }

  return <LandingPage onOpenDashboard={openDashboard} />;
}

export default App;
