import express, { type Request, Response, NextFunction } from "express";
import { registerRoutes } from "./routes";
import { serveStatic } from "./static";
import { createServer, request as httpRequest } from "http";
import { request as httpsRequest } from "https";

const app = express();
const httpServer = createServer(app);
const gatewayUrl =
  process.env.GATEWAY_URL ??
  (process.env.NODE_ENV === "production"
    ? "http://gateway:8080"
    : "http://localhost:8080");

const gatewayPrefixes = [
  "/auth",
  "/users",
  "/departments",
  "/positions",
  "/courses",
  "/progress",
  "/lessons",
  "/certificates",
  "/chat",
  "/notifications",
  "/analytics",
];

declare module "http" {
  interface IncomingMessage {
    rawBody: unknown;
  }
}

function proxyGateway(req: Request, res: Response) {
  const target = new URL(req.originalUrl, gatewayUrl);
  const proxyRequest = target.protocol === "https:" ? httpsRequest : httpRequest;
  const headers = { ...req.headers };
  delete headers.host;

  const forwarded = proxyRequest(
    target,
    {
      method: req.method,
      headers,
    },
    (proxyRes) => {
      res.status(proxyRes.statusCode ?? 502);
      Object.entries(proxyRes.headers).forEach(([key, value]) => {
        if (value !== undefined) {
          res.setHeader(key, value);
        }
      });
      proxyRes.pipe(res);
    },
  );

  forwarded.on("error", (error) => {
    if (!res.headersSent) {
      res.status(502).json({ message: `Gateway недоступен: ${error.message}` });
    } else {
      res.end();
    }
  });

  req.pipe(forwarded);
}

app.use((req, res, next) => {
  const shouldProxy = gatewayPrefixes.some(
    (prefix) => req.path === prefix || req.path.startsWith(`${prefix}/`),
  );

  if (shouldProxy) {
    proxyGateway(req, res);
    return;
  }

  next();
});

app.use(
  express.json({
    verify: (req, _res, buf) => {
      req.rawBody = buf;
    },
  }),
);

app.use(express.urlencoded({ extended: false }));

export function log(message: string, source = "express") {
  const formattedTime = new Date().toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  });

  console.log(`${formattedTime} [${source}] ${message}`);
}

app.use((req, res, next) => {
  const start = Date.now();
  const path = req.path;
  let capturedJsonResponse: Record<string, any> | undefined = undefined;

  const originalResJson = res.json;
  res.json = function (bodyJson, ...args) {
    capturedJsonResponse = bodyJson;
    return originalResJson.apply(res, [bodyJson, ...args]);
  };

  res.on("finish", () => {
    const duration = Date.now() - start;
    if (path.startsWith("/api")) {
      let logLine = `${req.method} ${path} ${res.statusCode} in ${duration}ms`;
      if (capturedJsonResponse) {
        logLine += ` :: ${JSON.stringify(capturedJsonResponse)}`;
      }

      log(logLine);
    }
  });

  next();
});

(async () => {
  await registerRoutes(httpServer, app);

  app.use((err: any, _req: Request, res: Response, _next: NextFunction) => {
    const status = err.status || err.statusCode || 500;
    const message = err.message || "Internal Server Error";

    res.status(status).json({ message });
    throw err;
  });

  // importantly only setup vite in development and after
  // setting up all the other routes so the catch-all route
  // doesn't interfere with the other routes
  if (process.env.NODE_ENV === "production") {
    serveStatic(app);
  } else {
    const { setupVite } = await import("./vite");
    await setupVite(httpServer, app);
  }

  // ALWAYS serve the app on the port specified in the environment variable PORT
  // Other ports are firewalled. Default to 5000 if not specified.
  // this serves both the API and the client.
  // It is the only port that is not firewalled.
  const port = parseInt(process.env.PORT || "5000", 10);
  httpServer.listen(
    {
      port,
      host: "0.0.0.0",
      reusePort: true,
    },
    () => {
      log(`serving on port ${port}`);
    },
  );
})();
