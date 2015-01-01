(ns gogd.core
  (:use [compojure.core :only (defroutes GET defroutes POST)]
        ring.util.response
        ring.middleware.cors
        org.httpkit.server)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [redirect]]
            [ring.middleware.reload :as reload]))

(def clients (atom {}))

(defn ws [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))

(defn transfer-data [req]
  (doseq [client @clients]
    (send! (key client) (slurp (:body req))))
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body "{\"success\": true}"})

(defroutes routes
  (GET "/data" [] ws)
  (POST "/data" [] transfer-data))

(def application (-> (handler/site routes)
                     reload/wrap-reload
                     (wrap-cors :access-control-allow-origin #".+")))

(defn -main [& args]
  (run-server application {:port 4808 :join? false}))
