(ns gogd.core
  (:use [compojure.core :only (defroutes GET defroutes POST)]
        ring.middleware.cors
        org.httpkit.server)
  (:require [compojure.handler :as handler]))

(def clients (atom #{}))

(defn ws [req]
  (with-channel req channel
    (swap! clients conj channel)
    (println channel " connected!")
    (on-close channel (fn [status]
                        (swap! clients disj channel)
                        (println channel " disconnected. status: " status)))))

(defn transfer-data [req]
  (let [data (slurp (:body req))]
    (doseq [client @clients]
      (send! client data)))
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"success\": true}"})

(defroutes routes
  (GET  "/data" [] ws)
  (POST "/data" [] transfer-data))

(def application (-> (handler/site routes)
                     (wrap-cors :access-control-allow-origin #".+")))

(defn -main [& args]
  (run-server application {:port 4808 :join? false}))
