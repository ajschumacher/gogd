(ns gogd.core
  (:use [compojure.core :only (defroutes GET)]
        ring.util.response
        ring.middleware.cors
        org.httpkit.server)
  (:require [compojure.handler :as handler]
            [ring.util.response :refer [redirect]]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]))

(def clients (atom {}))

(defn ws [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))

(future (loop []
          (doseq [client @clients]
            (send! (key client) (generate-string
                                 {:happiness (rand 10)})
                   false))
          (Thread/sleep 5000)
          (recur)))

(defroutes routes
  (GET "/data" [] ws))

(def application (-> (handler/site routes)
                     reload/wrap-reload
                     (wrap-cors
                      :access-control-allow-origin #".+")))

(defn -main [& args]
  (let [port (Integer/parseInt
               (or (System/getenv "PORT") "4808"))]
    (run-server application {:port port :join? false})))
